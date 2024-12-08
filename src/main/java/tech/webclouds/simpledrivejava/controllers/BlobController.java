package tech.webclouds.simpledrivejava.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tech.webclouds.simpledrivejava.configs.ApplicationProperties;
import tech.webclouds.simpledrivejava.helpers.StorageServiceFactory;
import tech.webclouds.simpledrivejava.models.entities.BlobMetadata;
import tech.webclouds.simpledrivejava.models.entities.BlobStorageType;
import tech.webclouds.simpledrivejava.models.packets.BlobRequest;
import tech.webclouds.simpledrivejava.models.packets.BlobResponse;
import tech.webclouds.simpledrivejava.repositories.BlobMetadataRepository;
import tech.webclouds.simpledrivejava.services.IStorageService;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/blobs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_USER')")
@Validated
public class BlobController {

    private final BlobMetadataRepository metadataRepository;
    private final StorageServiceFactory storageServiceFactory;
    private final BlobStorageType storageType;
    private final Validator validator;

    @Autowired
    public BlobController(BlobMetadataRepository metadataRepository, StorageServiceFactory storageServiceFactory,
                          ApplicationProperties applicationProperties, Validator validator) {
        this.metadataRepository = metadataRepository;
        this.storageServiceFactory = storageServiceFactory;
        this.storageType = BlobStorageType.valueOf(applicationProperties.getStorageType());
        this.validator = validator;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Blob", description = "Get blob by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Blob found", content = @Content(schema = @Schema(implementation = BlobResponse.class))),
            @ApiResponse(responseCode = "404", description = "Blob not found")
    })
    public ResponseEntity<?> getBlob(
            @Parameter(description = "The id of the blob", required = true)
            @PathVariable String id) {
        Optional<BlobMetadata> metadataOptional = metadataRepository.findByBlobId(id);

        if (metadataOptional.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Blob not found"));
        }

        BlobMetadata metadata = metadataOptional.get();
        byte[] data = storageServiceFactory.getStorageService(metadata.getStorageType()).getBlob(id, metadata.getContentType());

        String base64 = Base64.getEncoder().encodeToString(data);

        BlobResponse blobResponse = new BlobResponse(metadata.getBlobId(), base64, metadata.getSize(), metadata.getCreatedAt());
        return ResponseEntity.ok(blobResponse);
    }

    @GetMapping
    @Operation(summary = "Get Blobs", description = "Get all blobs")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Blobs found", content = @Content(schema = @Schema(implementation = BlobResponse.class)))
    })
    public List<BlobResponse> getBlobs(
            @Parameter(description = "The storage type of the blobs. Allowed values are: S3, Local, Database, Ftp")
            @RequestParam(required = false) String storageType) {
        List<BlobMetadata> blobs = (storageType != null) ? metadataRepository.findByStorageType(BlobStorageType.valueOf(storageType)) : metadataRepository.findAll();

        return blobs.stream().map(blob -> new BlobResponse(blob.getBlobId(), "", blob.getSize(), blob.getCreatedAt())).collect(Collectors.toList());
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Upload Blob", description = "Upload a blob")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Blob uploaded", content = @Content(schema = @Schema(implementation = BlobResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public ResponseEntity<?> uploadBlob(
            @Parameter(description = "Blob request", required = true, content = @Content(schema = @Schema(implementation = BlobRequest.class)))
            @Valid @RequestBody BlobRequest request) {

        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            return ResponseEntity.badRequest().body(violations.stream().map(ConstraintViolation::getMessage).toList());
        }

        // Validate for existing blob ID
        if (metadataRepository.existsByBlobId(request.id())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Blob Id already exists"));
        }

        // Extract content type and data
        var contentInfo = extractContentTypeAndData(request.data());
        String contentType = contentInfo.get("contentType");
        String base64Data = contentInfo.get("base64Data");

        byte[] data = Base64.getDecoder().decode(base64Data);

        BlobMetadata metadata = new BlobMetadata(null, request.id(), storageType, data.length, new Date(), contentType);
        metadata = metadataRepository.save(metadata);
        IStorageService storageService = storageServiceFactory.getStorageService(storageType);
        boolean isSaved = storageService.saveBlob(request.id(), data, contentType);
        if (!isSaved) {
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to store blob"));
        }
        String base64 = Base64.getEncoder().encodeToString(data);
        BlobResponse blobResponse = new BlobResponse(metadata.getBlobId(), base64, metadata.getSize(), metadata.getCreatedAt());
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
        map.put("contentType", contentType);
        map.put("base64Data", base64Data);
        return map;
    }
}
