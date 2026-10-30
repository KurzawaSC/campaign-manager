package com.kurzawski.campaigns.exception;

public class CampaignNotFoundException extends RuntimeException {

    public CampaignNotFoundException(Long id) {
        super("Campaign " + id + " not found");
    }
}
