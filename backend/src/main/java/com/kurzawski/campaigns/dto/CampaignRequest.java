package com.kurzawski.campaigns.dto;

import java.math.BigDecimal;
import java.util.Set;

import com.kurzawski.campaigns.model.CampaignStatus;
import com.kurzawski.campaigns.validation.MinBid;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CampaignRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull Long productId,
        @NotEmpty Set<@NotNull Long> keywordIds,
        @NotNull @MinBid @Digits(integer = 17, fraction = 2) BigDecimal bidAmount,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 17, fraction = 2) BigDecimal campaignFund,
        @NotNull CampaignStatus status,
        Long townId,
        @NotNull @Positive Integer radiusKm) {
}
