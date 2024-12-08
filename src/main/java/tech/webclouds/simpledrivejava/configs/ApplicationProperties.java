package tech.webclouds.simpledrivejava.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    private String storageType;
    private Logging logging;
    private UserAuth userAuth;
    private JwtToken jwtToken;
    private S3 s3;
    private Ftp ftp;
    private FileSystem fileSystem;
    private Testing testing;
    private String appSecret;

    @Data
    public static class Logging {
        private Map<String, String> logLevel;
    }

    @Data
    public static class UserAuth {
        private String username;
        private String hashedPassword;
    }

    @Data
    public static class JwtToken {
        private String tokenIssuer;
        private String jwtSecretKey;
        private String tokenAudience;
    }

    @Data
    public static class S3 {
        private String bucketUrl;
        private String accessKey;
        private String secretKey;
        private String region;
    }

    @Data
    public static class Ftp {
        private String ftpUrl;
        private String ftpUsername;
        private String ftpPassword;
    }

    @Data
    public static class FileSystem {
        private String storagePath;
    }

    @Data
    public static class Testing {
        private String filePath;
        private String serverUrl;
        private String fileHashValue;
        private String username;
        private String password;
    }
}
