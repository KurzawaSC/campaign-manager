package com.kurzawski.campaigns.dto;

import java.math.BigDecimal;
import java.util.Set;

import com.kurzawski.campaigns.model.CampaignStatus;
import com.kurzawski.campaigns.validation.MinBid;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CampaignRequest(
        @Schema(example = "Summer sale")
        @NotBlank @Size(max = 255) String name,

        @Schema(example = "1")
        @NotNull Long productId,

        @ArraySchema(schema = @Schema(example = "1"), minItems = 1)
        @NotEmpty Set<@NotNull Long> keywordIds,

        @Schema(example = "1.50", description = "At least the minimum bid amount from GET /api/config")
        @NotNull @MinBid @Digits(integer = 17, fraction = 2) BigDecimal bidAmount,

        @Schema(example = "100.00", description = "Deducted from the Emerald account")
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 17, fraction = 2) BigDecimal campaignFund,

        @NotNull CampaignStatus status,

        @Schema(example = "1", description = "Optional")
        Long townId,

        @Schema(example = "25", description = "Radius in kilometres")
        @NotNull @Positive Integer radiusKm) {
}
