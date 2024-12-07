package tech.webclouds.simpledrivejava.models.packets;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Date;

public record BlobResponse(
        String id,
        String data,
        long size,
        @JsonProperty("created_at")
        Date createdAt
) implements Serializable {
}
