package tech.webclouds.simpledrivejava.models.entities;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Entity
@Table(name = "BlobMetaData", uniqueConstraints = {@UniqueConstraint(columnNames = "blob_id")})
public class BlobMetadata implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "blob_id", nullable = false, length = 64)
    private String blobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", length = 10)
    private BlobStorageType storageType = BlobStorageType.Database;

    @Column(name = "size", nullable = false)
    private Integer size;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @Column(name = "content_type", length = 10)
    private String contentType;
}
