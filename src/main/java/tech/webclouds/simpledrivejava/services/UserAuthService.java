package tech.webclouds.simpledrivejava.services;

import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import tech.webclouds.simpledrivejava.configs.ApplicationProperties;

@Service
public class UserAuthService {
    private final String username;
    private final String hashedPassword;

    public UserAuthService(ApplicationProperties applicationProperties) {
        this.username = applicationProperties.getUserAuth().getUsername();
        this.hashedPassword = applicationProperties.getUserAuth().getHashedPassword();
    }

    /**
     * Verifies if the provided username and password match the stored credentials.
     *
     * @param username the input username
     * @param password the input password
     * @return true if the credentials match, false otherwise
     */
    public boolean verifyPassword(String username, String password) {
        return this.username.equals(username) && BCrypt.checkpw(password, this.hashedPassword);
    }
}
