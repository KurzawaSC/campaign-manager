package com.kurzawski.campaigns.exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(BigDecimal available, BigDecimal required) {
        super("Insufficient funds: available " + available + ", required " + required);
    }
}
