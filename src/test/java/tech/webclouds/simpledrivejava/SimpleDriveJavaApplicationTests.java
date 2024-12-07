package tech.webclouds.simpledrivejava;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SimpleDriveJavaApplicationTests {

    @Value("${S3.BucketUrl}")
    private String bucketUrl;

    @Value("${S3.Region}")
    private String region;

    @Value("${S3.AccessKey}")
    private String accessKey;

    @Value("${S3.SecretKey}")
    private String secretKey;

    @Value("${Testing.FilePath}")
    private String filePath;

    @Value("${Testing.ServerUrl}")
    private String serverUrl;

    @Value("${Testing.FileHashValue}")
    private String fileHashValue;

    @Value("${Testing.Username}")
    private String username;

    @Value("${Testing.Password}")
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
    public void uploadBlob_ShouldReturnOk() throws Exception {
        // Arrange
        byte[] fileContent = Files.readAllBytes(new File(filePath).toPath());
        String fileContentBase64 = Base64.getEncoder().encodeToString(fileContent);

        Map<String, String> blobRequest = new HashMap<>();
        blobRequest.put("id", UUID.randomUUID().toString());
        blobRequest.put("data", "data:image/jpeg;base64," + fileContentBase64);

        String token = getToken();

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(serverUrl + "/api/v1/blobs")).header("Content-Type", "application/json").header("Authorization", "Bearer " + token).POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(blobRequest))).build();

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

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(serverUrl + "/api/v1/blobs")).header("Content-Type", "application/json").header("Authorization", "Bearer " + token).POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(invalidBlobRequest))).build();

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

    private String getToken() throws Exception {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", username);
        loginRequest.put("password", password);

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(serverUrl + "/api/v1/auth/login")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(loginRequest))).build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(HttpStatus.OK.value(), response.statusCode());

        Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
        return (String) responseBody.get("token");
    }
}
