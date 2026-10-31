package com.kurzawski.campaigns.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @Schema(example = "Travel Mug", description = "Unique for the seller, ignoring case")
        @NotBlank @Size(max = 255) String name) {
}
