package com.kurzawski.campaigns.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kurzawski.campaigns.dto.AccountResponse;
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
        Long sellerId = currentSellerProvider.getCurrentSeller().getId();
        return accountRepository.findBySellerId(sellerId)
                .map(AccountResponse::from)
                .orElseThrow(() -> new IllegalStateException("No Emerald account for seller " + sellerId));
    }
}
