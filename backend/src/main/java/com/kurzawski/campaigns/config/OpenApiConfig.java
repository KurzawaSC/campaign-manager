package com.kurzawski.campaigns.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Campaign Manager API",
        version = "1.0",
        description = "Manage product campaigns funded from the seller's Emerald account."))
public class OpenApiConfig {
}
