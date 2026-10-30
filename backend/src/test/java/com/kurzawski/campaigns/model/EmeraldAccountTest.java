package com.kurzawski.campaigns.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.kurzawski.campaigns.exception.InsufficientFundsException;

class EmeraldAccountTest {

    private final EmeraldAccount account = new EmeraldAccount(new Seller("Test Seller"), new BigDecimal("100.00"));

    @Test
    void withdrawReducesBalance() {
        account.withdraw(new BigDecimal("30.50"));

        assertThat(account.getBalance()).isEqualByComparingTo("69.50");
    }

    @Test
    void withdrawAllowsTheWholeBalance() {
        account.withdraw(new BigDecimal("100.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void withdrawAboveBalanceFailsAndKeepsBalance() {
        assertThatThrownBy(() -> account.withdraw(new BigDecimal("100.01")))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void depositIncreasesBalance() {
        account.deposit(new BigDecimal("0.10"));

        assertThat(account.getBalance()).isEqualByComparingTo("100.10");
    }
}
