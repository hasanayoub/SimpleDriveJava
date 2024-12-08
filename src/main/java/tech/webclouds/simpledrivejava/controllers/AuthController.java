package tech.webclouds.simpledrivejava.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.webclouds.simpledrivejava.helpers.TokenAgent;
import tech.webclouds.simpledrivejava.models.packets.LoginRequest;
import tech.webclouds.simpledrivejava.models.packets.LoginResponse;
import tech.webclouds.simpledrivejava.services.UserAuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {

    private final UserAuthService userAuthService;
    private final TokenAgent tokenAgent;

    @Operation(summary = "Login", description = "Login to the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid username or password")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Login request", required = true, content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = LoginRequest.class)))
            @RequestBody
            LoginRequest request) {
        if (!userAuthService.verifyPassword(request.username(), request.password())) {
            return ResponseEntity.status(401).body(Map.of("Message", "Invalid username or password."));
        }
        String token = tokenAgent.generateJwtToken(request.username(), "ROLE_USER");
        return ResponseEntity.ok(Map.of("token", token));
    }
}

