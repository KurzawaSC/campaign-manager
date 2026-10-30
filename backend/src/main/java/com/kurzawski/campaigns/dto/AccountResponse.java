package com.kurzawski.campaigns.dto;

import java.math.BigDecimal;

import com.kurzawski.campaigns.model.EmeraldAccount;

public record AccountResponse(BigDecimal balance) {

    public static AccountResponse from(EmeraldAccount account) {
        return new AccountResponse(account.getBalance());
    }
}
