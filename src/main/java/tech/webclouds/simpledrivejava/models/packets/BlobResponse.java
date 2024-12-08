package tech.webclouds.simpledrivejava.models.packets;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;

import java.io.Serializable;
import java.util.Date;

public record BlobResponse(
        @Operation(description = "The id of the blob")
        String id,
        @Operation(description = "The data of the blob in base64 format")
        String data,
        @Operation(description = "The size of the blob")
        long size,
        @Operation(description = "The content type of the blob, for example, application/json")
        @JsonProperty("created_at")
        Date createdAt
) implements Serializable {
}
