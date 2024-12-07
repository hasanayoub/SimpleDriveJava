package tech.webclouds.simpledrivejava.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import tech.webclouds.simpledrivejava.models.entities.BlobMetadata;
import tech.webclouds.simpledrivejava.models.entities.BlobStorageType;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlobMetadataRepository extends JpaRepository<BlobMetadata, Long>, JpaSpecificationExecutor<BlobMetadata> {

    Optional<BlobMetadata> findByBlobId(String id);
    boolean existsByBlobId(String blobId);
    List<BlobMetadata> findByStorageType(BlobStorageType storageType);
}
