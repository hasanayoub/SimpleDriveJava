package tech.webclouds.simpledrivejava.controllers;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.webclouds.simpledrivejava.configs.ApplicationProperties;
import tech.webclouds.simpledrivejava.helpers.StorageServiceFactory;
import tech.webclouds.simpledrivejava.models.entities.BlobMetadata;
import tech.webclouds.simpledrivejava.models.entities.BlobStorageType;
import tech.webclouds.simpledrivejava.models.packets.BlobRequest;
import tech.webclouds.simpledrivejava.models.packets.BlobResponse;
import tech.webclouds.simpledrivejava.repositories.BlobMetadataRepository;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/blobs")
@RequiredArgsConstructor
public class BlobController {

    private final BlobMetadataRepository metadataRepository;
    private final StorageServiceFactory storageServiceFactory;
    private final BlobStorageType storageType;

    @Autowired
    public BlobController(BlobMetadataRepository metadataRepository, StorageServiceFactory storageServiceFactory, ApplicationProperties applicationProperties) {
        this.metadataRepository = metadataRepository;
        this.storageServiceFactory = storageServiceFactory;
        this.storageType = BlobStorageType.valueOf(applicationProperties.getStorageType());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBlob(@PathVariable String id) {
        Optional<BlobMetadata> metadataOptional = metadataRepository.findByBlobId(id);
        if (metadataOptional.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Blob not found"));
        }

        BlobMetadata metadata = metadataOptional.get();
        byte[] data = storageServiceFactory
                .getStorageService(metadata.getStorageType())
                .getBlob(id, metadata.getContentType());

        String base64 = Base64.getEncoder().encodeToString(data);

        BlobResponse blobResponse = new BlobResponse(
                metadata.getBlobId(),
                base64,
                metadata.getSize(),
                metadata.getCreatedAt()
        );
        return ResponseEntity.ok(blobResponse);
    }

    @GetMapping
    public List<BlobResponse> getBlobs(@RequestParam(required = false) String storageType) {
        List<BlobMetadata> blobs = (storageType != null)
                ? metadataRepository.findByStorageType(BlobStorageType.valueOf(storageType))
                : metadataRepository.findAll();

        return blobs.stream()
                .map(blob -> new BlobResponse(blob.getBlobId(), "", blob.getSize(), blob.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> uploadBlob(@Valid @RequestBody BlobRequest request) {
        // Validate for existing blob ID
        if (metadataRepository.existsByBlobId(request.id())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Blob Id already exists"));
        }

        // Extract content type and data
        var contentInfo = extractContentTypeAndData(request.data());
        String contentType = contentInfo.get("contentType");
        String base64Data = contentInfo.get("base64Data");

        byte[] data = Base64.getDecoder().decode(base64Data);

        BlobMetadata metadata = new BlobMetadata(
                null,
                request.id(),
                storageType,
                data.length,
                new Date(),
                contentType
        );
        metadataRepository.save(metadata);
        boolean isSaved = storageServiceFactory.getStorageService(storageType).saveBlob(request.id(), data, contentType);
        if (!isSaved) {
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to store blob"));
        }
        String base64 = Base64.getEncoder().encodeToString(data);
        BlobResponse blobResponse = new BlobResponse(
                metadata.getBlobId(),
                base64,
                metadata.getSize(),
                metadata.getCreatedAt()
        );
        return ResponseEntity.ok(blobResponse);
    }

    public static HashMap<String, String> extractContentTypeAndData(String base64Input) {
        // Check if the Base64 string includes content type
        if (!base64Input.startsWith("data:")) {
            return HashMap.newHashMap(0);
        }

        // Split the input to extract content type and Base64 data
        String[] split = base64Input.split(";");
        String contentType = split[0].replace("data:", "");
        String base64Data = split[1].replace("base64,", "");
        var map = new HashMap<String, String>();
        map.put("ContentType", contentType);
        map.put("Base64Data", base64Data);
        return map;
    }
}
