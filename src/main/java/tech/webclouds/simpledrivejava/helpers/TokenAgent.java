package tech.webclouds.simpledrivejava.helpers;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import tech.webclouds.simpledrivejava.configs.ApplicationProperties;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class TokenAgent {

    private final SecretKey secretKey;
    private final String issuer;
    private final String audience;

    public TokenAgent(ApplicationProperties applicationProperties) {
        this.secretKey = Keys.hmacShaKeyFor(applicationProperties.getJwtToken().getJwtSecretKey().getBytes(StandardCharsets.UTF_8));
        this.issuer = applicationProperties.getJwtToken().getTokenIssuer();
        this.audience = applicationProperties.getJwtToken().getTokenAudience();
    }

    public String generateJwtToken(String username, String role) {
        return Jwts.builder()
                .subject(username)
                .id(UUID.randomUUID().toString())
                .claim("role", role)
                .issuer(issuer)
                .expiration(new Date(System.currentTimeMillis() + 3600 * 1000)) // 1 hour expiration
                .audience().add(audience).and()
                .signWith(secretKey)
                .compact();
    }
}
