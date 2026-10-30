package com.kurzawski.campaigns.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kurzawski.campaigns.dto.AccountResponse;
import com.kurzawski.campaigns.model.EmeraldAccount;
import com.kurzawski.campaigns.repository.EmeraldAccountRepository;

@Service
public class AccountService {

    private final EmeraldAccountRepository accountRepository;
    private final CurrentSellerProvider currentSellerProvider;

    public AccountService(EmeraldAccountRepository accountRepository, CurrentSellerProvider currentSellerProvider) {
        this.accountRepository = accountRepository;
        this.currentSellerProvider = currentSellerProvider;
    }

    @Transactional(readOnly = true)
    public AccountResponse getCurrentAccount() {
        return AccountResponse.from(findCurrentAccount());
    }

    @Transactional
    public void withdraw(BigDecimal amount) {
        findCurrentAccount().withdraw(amount);
    }

    @Transactional
    public void deposit(BigDecimal amount) {
        findCurrentAccount().deposit(amount);
    }

    private EmeraldAccount findCurrentAccount() {
        Long sellerId = currentSellerProvider.getCurrentSeller().getId();
        return accountRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new IllegalStateException("No Emerald account for seller " + sellerId));
    }
}
