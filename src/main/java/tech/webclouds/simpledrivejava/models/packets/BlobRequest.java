package tech.webclouds.simpledrivejava.models.packets;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;

import java.io.Serializable;

@Validated
public record BlobRequest(
        @Parameter(description = "The id of the blob. It can be any value but must be unique", required = true)
        @JsonProperty("id")
        @NotEmpty(message = "Invalid input. Id is required.")
        @Size(max = 50, message = "Invalid input. Id must be less than 50 characters.")
        String id,

        @Parameter(description = "The data of the blob in base64 format.", required = true)
        @JsonProperty("data")
        @NotEmpty(message = "Invalid input. Data is required.")
        @Pattern(regexp = "^data:image/(png|jpeg|jpg|gif);base64,[A-Za-z0-9+/=]+$", message = "Invalid input. Data must be a valid base64 image.")
        String data
) implements Serializable {
}
