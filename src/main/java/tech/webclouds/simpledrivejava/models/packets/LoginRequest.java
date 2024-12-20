package tech.webclouds.simpledrivejava.models.packets;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.io.Serializable;


public record LoginRequest(
        @Parameter(description = "Username", required = true)
        @NotEmpty(message = "Invalid input. username is required.")
        @Size(max = 50, message = "Invalid input. Username must be less than 50 characters.")
        String username,

        @Parameter(description = "Password", required = true)
        @NotEmpty(message = "Invalid input. Password is required.")
        @Size(max = 50, message = "Invalid input. Password must be less than 50 characters.")
        String password
) implements Serializable {
}
