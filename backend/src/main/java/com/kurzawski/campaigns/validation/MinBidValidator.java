package com.kurzawski.campaigns.validation;

import java.math.BigDecimal;

import com.kurzawski.campaigns.config.CampaignProperties;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MinBidValidator implements ConstraintValidator<MinBid, BigDecimal> {

    private final CampaignProperties campaignProperties;

    public MinBidValidator(CampaignProperties campaignProperties) {
        this.campaignProperties = campaignProperties;
    }

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null || value.compareTo(campaignProperties.minBidAmount()) >= 0) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("must be at least " + campaignProperties.minBidAmount())
                .addConstraintViolation();
        return false;
    }
}
