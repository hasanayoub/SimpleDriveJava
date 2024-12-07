package tech.webclouds.simpledrivejava.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class JwtConfig {

    private final ApplicationProperties applicationProperties;

    public JwtConfig(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Convert the secret key string into a SecretKey object
        byte[] keyBytes = applicationProperties.getJwtToken().getJwtSecretKey().getBytes(StandardCharsets.UTF_8);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");

        // Create the JwtDecoder using the secret key
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }
}
