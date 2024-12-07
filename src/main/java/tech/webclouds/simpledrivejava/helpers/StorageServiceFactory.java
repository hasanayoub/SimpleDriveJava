package tech.webclouds.simpledrivejava.helpers;

import org.springframework.stereotype.Service;
import tech.webclouds.simpledrivejava.models.entities.BlobStorageType;
import tech.webclouds.simpledrivejava.services.*;

@Service
public class StorageServiceFactory {

    private final S3StorageService s3StorageService;
    private final LocalFileStorageService localFileStorageService;
    private final DatabaseStorageService databaseStorageService;
    private final FtpStorageService ftpStorageService;

    public StorageServiceFactory(
            S3StorageService s3StorageService,
            LocalFileStorageService localFileStorageService,
            DatabaseStorageService databaseStorageService,
            FtpStorageService ftpStorageService) {
        this.s3StorageService = s3StorageService;
        this.localFileStorageService = localFileStorageService;
        this.databaseStorageService = databaseStorageService;
        this.ftpStorageService = ftpStorageService;
    }

    public IStorageService getStorageService(BlobStorageType storageType) {
        return switch (storageType) {
            case S3 -> s3StorageService;
            case Local -> localFileStorageService;
            case Database -> databaseStorageService;
            case Ftp -> ftpStorageService;
        };
    }
}
