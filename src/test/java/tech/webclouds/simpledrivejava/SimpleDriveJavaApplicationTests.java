package tech.webclouds.simpledrivejava;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.lang.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import tech.webclouds.simpledrivejava.helpers.AwsV4Agent;
import tech.webclouds.simpledrivejava.models.packets.BlobResponse;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest("spring.profiles.active=localhost")
class SimpleDriveJavaApplicationTests {

    @Value("${app.StorageType}")
    private String storageType;

    @Value("${app.S3.BucketUrl}")
    private String bucketUrl;

    @Value("${app.S3.Region}")
    private String region;

    @Value("${app.S3.AccessKey}")
    private String accessKey;

    @Value("${app.S3.SecretKey}")
    private String secretKey;

    @Value("${app.Testing.FilePath}")
    private String filePath;

    @Value("${app.Testing.ServerUrl}")
    private String serverUrl;

    @Value("${app.Testing.FileHashValue}")
    private String fileHashValue;

    @Value("${app.Testing.Username}")
    private String username;

    @Value("${app.Testing.Password}")
    private String password;

    private HttpClient httpClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Test
    public void uploadBlob_TestHash() throws Exception {
        byte[] fileContent = Files.readAllBytes(new File(filePath).toPath());
        String calculatedHash = AwsV4Agent.hash(fileContent);
        assertEquals(fileHashValue, calculatedHash);
    }

    @Test
    public void uploadBlob_CheckLogin() throws Exception {
        String token = getToken();
        assertNotNull(token);
    }

    @Test
    public void uploadBlob_CheckS3Upload() throws Exception {
        // Arrange
        byte[] fileContent = Files.readAllBytes(new File(filePath).toPath());
        String id = UUID.randomUUID().toString();

        AwsV4Agent awsV4Agent = new AwsV4Agent("s3", region, accessKey, secretKey);
        HttpRequest put = awsV4Agent.prepareRequest(bucketUrl + "/files/" + id, "PUT", fileContent);

        try (var httpClient = HttpClient.newHttpClient()) {
            HttpResponse<Void> response = httpClient.send(put, HttpResponse.BodyHandlers.discarding());
            System.out.println("Response Body: " + response.body());
            System.out.println("Status Code: " + response.statusCode());
            Assert.isTrue(response.statusCode() == 200 || response.statusCode() == 204);
        }
    }

    @Test
    public void uploadBlob_ShouldReturnOk() throws Exception {
        // Arrange
        byte[] fileContent = Files.readAllBytes(new File(filePath).toPath());
        String fileContentBase64 = Base64.getEncoder().encodeToString(fileContent);

        Map<String, String> blobRequest = new HashMap<>();
        blobRequest.put("id", UUID.randomUUID().toString());
        blobRequest.put("data", "data:image/jpeg;base64," + fileContentBase64);

        String json = objectMapper.writeValueAsString(blobRequest);

        String token = getToken();

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(serverUrl + "/api/v1/blobs"))
                .header("Content-Type", "application/json").header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        // Act
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Assert
        assertEquals(HttpStatus.OK.value(), response.statusCode());
        Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
        assertNotNull(responseBody.get("id"));
    }

    @Test
    public void uploadBlob_ShouldReturn401Response() throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(serverUrl + "/api/v1/blobs")).GET().build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.statusCode());
    }

    @Test
    public void uploadBlob_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        // Arrange
        Map<String, String> invalidBlobRequest = new HashMap<>();
        invalidBlobRequest.put("id", "");
        invalidBlobRequest.put("data", "");

        String token = getToken();

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(serverUrl + "/api/v1/blobs"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(invalidBlobRequest)))
                .build();

        // Act
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
    }

    @Test
    public void uploadBlob_GetBlobsList() throws Exception {
        String token = getToken();

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(serverUrl + "/api/v1/blobs")).header("Authorization", "Bearer " + token).GET().build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(HttpStatus.OK.value(), response.statusCode());
        BlobResponse[] blobs = objectMapper.readValue(response.body(), BlobResponse[].class);
        assertTrue(blobs.length > 0);
    }

    @Test
    public void uploadBlob_GetBlobsListData() throws Exception {

        // Get Token
        String token = getToken();
        HttpClient client = HttpClient.newHttpClient();

        // Set Authorization Header
        HttpRequest listRequest = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "/api/v1/blobs?storageType=" + storageType))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> listResponse = client.send(listRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println("Response: " + listResponse.statusCode());

        // Assert
        Assertions.assertEquals(200, listResponse.statusCode());

        // Parse JSON response
        ObjectMapper objectMapper = new ObjectMapper();
        BlobResponse[] blobsMetaData = objectMapper.readValue(listResponse.body(), BlobResponse[].class);

        if (blobsMetaData != null) {
            int blobCount = blobsMetaData.length;
            System.out.println("Blobs: " + blobCount);

            List<BlobResponse> blobsWithData = new ArrayList<>();
            boolean foundDataBytes = false;
            for (BlobResponse blob : blobsMetaData) {
                HttpRequest blobRequest = HttpRequest.newBuilder()
                        .uri(URI.create(serverUrl + "/api/v1/blobs/" + blob.id()))
                        .header("Authorization", "Bearer " + token)
                        .GET()
                        .build();

                HttpResponse<String> blobResponse = client.send(blobRequest, HttpResponse.BodyHandlers.ofString());
                System.out.println("Blob Data: " + blobResponse.statusCode());
                if (blobResponse.statusCode() == 200) {
                    BlobResponse blobMetaData = objectMapper.readValue(blobResponse.body(), BlobResponse.class);
                    if (blobMetaData != null) blobsWithData.add(blobMetaData);
                    foundDataBytes |= blobResponse.statusCode() == 200;
                }
            }
            Assertions.assertTrue(foundDataBytes);
        } else {
            Assertions.fail("Blob metadata is null");
        }
    }

    private String getToken() throws Exception {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", username);
        loginRequest.put("password", password);

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(serverUrl + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(loginRequest)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(HttpStatus.OK.value(), response.statusCode());

        Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
        return (String) responseBody.get("token");
    }
}
