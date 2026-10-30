package com.kurzawski.campaigns.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.campaign")
public record CampaignProperties(BigDecimal minBidAmount) {
}
