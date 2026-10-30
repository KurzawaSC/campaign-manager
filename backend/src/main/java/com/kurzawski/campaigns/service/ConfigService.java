package com.kurzawski.campaigns.service;

import org.springframework.stereotype.Service;

import com.kurzawski.campaigns.config.CampaignProperties;
import com.kurzawski.campaigns.dto.ConfigResponse;

@Service
public class ConfigService {

    private final CampaignProperties campaignProperties;

    public ConfigService(CampaignProperties campaignProperties) {
        this.campaignProperties = campaignProperties;
    }

    public ConfigResponse getConfig() {
        return new ConfigResponse(campaignProperties.minBidAmount());
    }
}
