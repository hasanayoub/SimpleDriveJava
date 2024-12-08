package tech.webclouds.simpledrivejava.models.packets;

import io.swagger.v3.oas.annotations.Parameter;

import java.io.Serializable;


@SuppressWarnings("unused")
public record LoginResponse(
        @Parameter(description = "Token", required = true)
        String token
) implements Serializable {
}
