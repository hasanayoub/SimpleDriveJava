package tech.webclouds.simpledrivejava.services;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.stereotype.Service;
import tech.webclouds.simpledrivejava.configs.ApplicationProperties;
import tech.webclouds.simpledrivejava.errors.CustomProblemException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@SuppressWarnings("DuplicatedCode")
@Service
public class FtpStorageService implements IStorageService {

    private final String ftpServerUrl;
    private final String ftpUsername;
    private final String ftpPassword;

    public FtpStorageService(ApplicationProperties applicationProperties) {
        this.ftpServerUrl = applicationProperties.getFtp().getFtpUrl().endsWith("/") ? applicationProperties.getFtp().getFtpUrl() : applicationProperties.getFtp().getFtpUrl() + "/";
        this.ftpUsername = applicationProperties.getFtp().getFtpUsername();
        this.ftpPassword = applicationProperties.getFtp().getFtpPassword();
    }

    @Override
    public boolean saveBlob(String id, byte[] data, String contentType) {
        try {
            FTPClient ftpClient = new FTPClient();

            try {
                String ext = LocalFileStorageService.getExtFromMimeType(contentType);
                // Connect and login
                var serverUrl = ftpServerUrl != null && ftpServerUrl.startsWith("ftp://") ? ftpServerUrl.replace("ftp://", "") : ftpServerUrl;
                if (serverUrl.endsWith("/")) {
                    serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
                }
                ftpClient.connect(serverUrl);
                ftpClient.login(ftpUsername, ftpPassword);
                System.out.println("Connected to FTP server!");

                // Set FTP mode
                ftpClient.enterLocalPassiveMode(); // Use passive mode if behind a firewall
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
//                ftpClient.setFileTransferMode(FTP.BINARY_FILE_TYPE);
                ftpClient.enterLocalPassiveMode();

                // Upload a file
                String remoteFile = id + ext;
                try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
                    boolean done = ftpClient.storeFile(remoteFile, inputStream);
                    if (done) {
                        System.out.println("File uploaded successfully!");
                    } else {
                        System.out.println("File upload failed.");
                    }
                    return true; // Assume success if no exception occurs
                }
            } catch (IOException e) {
                e.printStackTrace(System.out);
            } finally {
                try {
                    if (ftpClient.isConnected()) {
                        ftpClient.logout();
                        ftpClient.disconnect();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace(System.out);
                }
            }
        } catch (Exception ex) {
            System.err.println("Error uploading blob to FTP: " + ex.getMessage());
        }
        return false;
    }

    @Override
    public byte[] getBlob(String id, String contentType) {
        FTPClient ftpClient = new FTPClient();

        try {
            // Get file extension from content type
            String ext = LocalFileStorageService.getExtFromMimeType(contentType);
            String remoteFile = id + ext;

            // Connect and login
            var serverUrl = ftpServerUrl != null && ftpServerUrl.startsWith("ftp://") ? ftpServerUrl.replace("ftp://", "") : ftpServerUrl;
            if (serverUrl.endsWith("/")) {
                serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
            }

            ftpClient.connect(serverUrl);
            ftpClient.login(ftpUsername, ftpPassword);
            System.out.println("Connected to FTP server!");

            // Set FTP mode
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            // Download the file
            try (var outputStream = new ByteArrayOutputStream()) {
                boolean success = ftpClient.retrieveFile(remoteFile, outputStream);
                if (!success) {
                    throw new CustomProblemException(404, "File not found on FTP server: " + remoteFile);
                }
                return outputStream.toByteArray();
            }
        } catch (IOException ex) {
            throw new CustomProblemException(500, "Error downloading blob from FTP: " + ex.getMessage());
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                System.err.println("Error closing FTP connection: " + ex.getMessage());
            }
        }
    }
}
