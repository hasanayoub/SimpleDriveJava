package tech.webclouds.simpledrivejava.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.webclouds.simpledrivejava.models.entities.BlobData;
import tech.webclouds.simpledrivejava.repositories.BlobDataRepository;

import java.util.Optional;

@Service
public class DatabaseStorageService implements IStorageService {

    private final BlobDataRepository blobDataRepository;

    public DatabaseStorageService(BlobDataRepository blobDataRepository) {
        this.blobDataRepository = blobDataRepository;
    }

    @Override
    @Transactional
    public boolean saveBlob(String id, byte[] data, String contentType) {
        BlobData blob = new BlobData();
        blob.setBlobId(id);
        blob.setMediumBlobData(data);
        blob.setContentType(contentType);

        blobDataRepository.save(blob);
        return true;
    }

    @Override
    public byte[] getBlob(String id, String contentType) {
        Optional<BlobData> blob = blobDataRepository.findByBlobId(id);
        return blob.map(BlobData::getMediumBlobData).orElse(null);
    }
}
