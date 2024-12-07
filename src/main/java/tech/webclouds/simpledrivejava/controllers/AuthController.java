package tech.webclouds.simpledrivejava.controllers;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.webclouds.simpledrivejava.helpers.TokenAgent;
import tech.webclouds.simpledrivejava.models.packets.LoginRequest;
import tech.webclouds.simpledrivejava.services.UserAuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {

    private final UserAuthService userAuthService;
    private final TokenAgent tokenAgent;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (!userAuthService.verifyPassword(request.username(), request.password())) {
            return ResponseEntity.status(401).body(Map.of("Message", "Invalid username or password."));
        }
        String token = tokenAgent.generateJwtToken(request.username());
        return ResponseEntity.ok(Map.of("Token", token));
    }
}

