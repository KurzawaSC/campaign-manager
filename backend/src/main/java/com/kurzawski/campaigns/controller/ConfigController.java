package com.kurzawski.campaigns.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kurzawski.campaigns.dto.ConfigResponse;
import com.kurzawski.campaigns.service.ConfigService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/config")
@Tag(name = "Config", description = "Settings used by the campaign form")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    @Operation(summary = "Get form settings", description = "Returns the minimum bid amount.")
    public ConfigResponse getConfig() {
        return configService.getConfig();
    }
}
