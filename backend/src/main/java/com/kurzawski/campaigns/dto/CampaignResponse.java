package com.kurzawski.campaigns.dto;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import com.kurzawski.campaigns.model.Campaign;
import com.kurzawski.campaigns.model.CampaignStatus;

public record CampaignResponse(
        Long id,
        String name,
        ProductResponse product,
        List<KeywordResponse> keywords,
        BigDecimal bidAmount,
        BigDecimal campaignFund,
        CampaignStatus status,
        TownResponse town,
        Integer radiusKm) {

    public static CampaignResponse from(Campaign campaign) {
        return new CampaignResponse(
                campaign.getId(),
                campaign.getName(),
                ProductResponse.from(campaign.getProduct()),
                campaign.getKeywords().stream()
                        .map(KeywordResponse::from)
                        .sorted(Comparator.comparing(KeywordResponse::name))
                        .toList(),
                campaign.getBidAmount(),
                campaign.getCampaignFund(),
                campaign.getStatus(),
                campaign.getTown() == null ? null : TownResponse.from(campaign.getTown()),
                campaign.getRadiusKm());
    }
}
