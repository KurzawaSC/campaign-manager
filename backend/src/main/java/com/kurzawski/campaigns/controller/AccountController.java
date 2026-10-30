package com.kurzawski.campaigns.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kurzawski.campaigns.dto.AccountResponse;
import com.kurzawski.campaigns.service.AccountService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/account")
@Tag(name = "Account", description = "Seller's Emerald account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    @Operation(summary = "Get the Emerald account balance")
    public AccountResponse getAccount() {
        return accountService.getCurrentAccount();
    }
}
