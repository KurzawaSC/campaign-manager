package com.kurzawski.campaigns.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kurzawski.campaigns.dto.ConfigResponse;
import com.kurzawski.campaigns.service.ConfigService;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    public ConfigResponse getConfig() {
        return configService.getConfig();
    }
}
