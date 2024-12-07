package tech.webclouds.simpledrivejava.services;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import tech.webclouds.simpledrivejava.configs.ApplicationProperties;
import tech.webclouds.simpledrivejava.helpers.AwsV4Agent;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class S3StorageService implements IStorageService {

    private final String bucketUrl;
    private final String accessKey;
    private final String secretKey;
    private final String region;

    public S3StorageService(ApplicationProperties applicationProperties) {
        this.bucketUrl = applicationProperties.getS3().getBucketUrl().endsWith("/") ? applicationProperties.getS3().getBucketUrl() : applicationProperties.getS3().getBucketUrl() + "/";
        this.accessKey = applicationProperties.getS3().getAccessKey();
        this.secretKey = applicationProperties.getS3().getSecretKey();
        this.region = applicationProperties.getS3().getRegion();
    }

    @Override
    public boolean saveBlob(String id, byte[] data, String contentType) {
        try {
            createBucketIfNotFound("files");
            URI uri = URI.create(bucketUrl + "files/" + id);
            AwsV4Agent aws = new AwsV4Agent("s3", region, accessKey, secretKey);
            HttpRequest request = aws.prepareRequest(uri.toString(), "PUT", data);

            try (var httpClient = HttpClient.newHttpClient()) {
                HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                return response.statusCode() == 200 || response.statusCode() == 204;
            }
        } catch (Exception ex) {
            System.err.println("Error saving blob to S3: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public byte[] getBlob(String id, String contentType) {
        try {
            URI uri = URI.create(bucketUrl + "files/" + id);
            AwsV4Agent aws = new AwsV4Agent("s3", region, accessKey, secretKey);
            HttpRequest request = aws.prepareRequest(uri.toString(), "GET", null);

            try (var httpClient = HttpClient.newHttpClient()) {
                HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() != 200) {
                    return null;
                }
                return response.body();
            }
        } catch (Exception ex) {
            System.err.println("Error retrieving blob from S3: " + ex.getMessage());
            return null;
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void createBucketIfNotFound(String bucketName) throws Exception {
        AwsV4Agent aws = new AwsV4Agent("s3", region, accessKey, secretKey);
        URI uri = URI.create(bucketUrl);
        HttpRequest request = aws.prepareRequest(uri.toString(), "GET", null);

        try (var httpClient = HttpClient.newHttpClient()) {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new ByteArrayInputStream(response.body().getBytes()));
                NodeList bucketNodes = document.getElementsByTagName("Name");
                for (int i = 0; i < bucketNodes.getLength(); i++) {
                    if (bucketNodes.item(i).getTextContent().equals(bucketName)) {
                        return; // Bucket exists
                    }
                }

                // If a bucket doesn't exist, create it
                URI bucketUri = URI.create(bucketUrl + bucketName);
                request = aws.prepareRequest(bucketUri.toString(), "PUT", null);
                httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            }
        }
    }
}
