package tech.webclouds.simpledrivejava.services;

import org.springframework.stereotype.Service;
import tech.webclouds.simpledrivejava.configs.ApplicationProperties;
import tech.webclouds.simpledrivejava.errors.CustomProblemException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

@Service
public class FtpStorageService implements IStorageService {

    private final String ftpServerUrl;
    private final String ftpUsername;
    private final String ftpPassword;

    public FtpStorageService(ApplicationProperties applicationProperties) {
        this.ftpServerUrl = applicationProperties.getFtp().getFtpUrl();
        this.ftpUsername = applicationProperties.getFtp().getFtpUsername();
        this.ftpPassword = applicationProperties.getFtp().getFtpPassword();
    }

    @Override
    public boolean saveBlob(String id, byte[] data, String contentType) {
        try {
            // Get file extension from content type
            String ext = LocalFileStorageService.getExtFromMimeType(contentType);
            URI uri = URI.create(ftpServerUrl + id + ext);

            // Create the basic authentication header
            String auth = ftpUsername + ":" + ftpPassword;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            // Create the HTTP request
            HttpRequest request = HttpRequest.newBuilder().uri(uri)
                    .header("Authorization", "Basic " + encodedAuth)
                    .header("Content-Type", contentType)
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(data))
                    .build();

            // Send the HTTP request
            try (HttpClient client = HttpClient.newHttpClient()) {
                HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                // Check the response status code
                return response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 204;
            }
        } catch (Exception ex) {
            System.err.println("Error uploading blob to FTP: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public byte[] getBlob(String id, String contentType) {
        try {
            // Get file extension from content type
            String ext = LocalFileStorageService.getExtFromMimeType(contentType);
            URI uri = URI.create(ftpServerUrl + id + ext);

            // Create the basic authentication header
            String auth = ftpUsername + ":" + ftpPassword;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            // Create the HTTP request
            HttpRequest request = HttpRequest.newBuilder().uri(uri).header("Authorization", "Basic " + encodedAuth).GET().build();

            // Send the HTTP request and get the response
            try (HttpClient client = HttpClient.newHttpClient()) {
                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

                // Check the response status code and return the body if successful
                if (response.statusCode() == 200) {
                    return response.body();
                } else {
                    throw new CustomProblemException(response.statusCode(), "Error downloading blob from FTP: HTTP status ");
                }
            }
        } catch (Exception ex) {
            throw new CustomProblemException(500, "Error downloading blob from FTP: " + ex.getMessage());
        }
    }
}
