package tech.webclouds.simpledrivejava.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import tech.webclouds.simpledrivejava.models.entities.BlobData;

import java.util.Optional;

@Repository
public interface BlobDataRepository extends JpaRepository<BlobData, Long>, JpaSpecificationExecutor<BlobData> {

    Optional<BlobData> findByBlobId(String id);
}
