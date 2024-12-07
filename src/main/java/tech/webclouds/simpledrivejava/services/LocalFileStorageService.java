package tech.webclouds.simpledrivejava.services;

import org.springframework.stereotype.Service;
import tech.webclouds.simpledrivejava.configs.ApplicationProperties;
import tech.webclouds.simpledrivejava.errors.CustomProblemException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

@Service
public class LocalFileStorageService implements IStorageService {

    private final String storagePath;

    public LocalFileStorageService(ApplicationProperties applicationProperties) {
        this.storagePath = applicationProperties.getFileSystem().getStoragePath();

        // Ensure the storage directory exists
        File directory = new File(storagePath);
        if (!directory.exists()) {
            boolean mkdirs = directory.mkdirs();
            System.out.println(mkdirs);
        }
    }

    @Override
    public boolean saveBlob(String id, byte[] data, String contentType) {
        try {
            // Get the file extension based on the content type
            String ext = getExtFromMimeType(contentType);

            // Construct the file path
            Path filePath = Path.of(storagePath, id + ext);

            // Write the data to the file
            Files.write(filePath, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return true;
        } catch (IOException e) {
            throw new CustomProblemException(500, "Error saving blob: " + e.getMessage());
        }
    }

    @Override
    public byte[] getBlob(String id, String contentType) {
        try {
            // Get the file extension based on the content type
            String ext = getExtFromMimeType(contentType);

            // Construct the file path
            Path filePath = Path.of(storagePath, id + ext);

            // Read the file and return its contents as a byte array
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            System.err.println("Error reading blob: " + e.getMessage());
            return null;
        }
    }

    // Map of MIME types to file extensions
    private static final Map<String, String> mimeToExtMap = new HashMap<>() {{
        put("application/pdf", ".pdf");
        put("image/jpeg", ".jpg");
        put("image/png", ".png");
        put("text/plain", ".txt");
        put("text/html", ".html");
        put("application/json", ".json");
        put("application/xml", ".xml");
        put("application/zip", ".zip");
    }};

    // Map of file extensions to MIME types
    private static final Map<String, String> extToMimeMap = new HashMap<>() {{
        put(".pdf", "application/pdf");
        put(".jpg", "image/jpeg");
        put(".jpeg", "image/jpeg");
        put(".png", "image/png");
        put(".txt", "text/plain");
        put(".html", "text/html");
        put(".json", "application/json");
        put(".xml", "application/xml");
        put(".zip", "application/zip");
    }};

    public static String getExtFromMimeType(String contentType) {
        return mimeToExtMap.getOrDefault(contentType, ".bin"); // Default to .bin for unknown types
    }

    @SuppressWarnings("unused")
    public static String getMimeTypeFromExt(String ext) {
        return extToMimeMap.getOrDefault(ext.toLowerCase(), "application/octet-stream"); // Default MIME type
    }
}
