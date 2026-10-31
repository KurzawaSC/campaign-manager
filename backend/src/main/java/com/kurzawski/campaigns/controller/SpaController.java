package com.kurzawski.campaigns.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import io.swagger.v3.oas.annotations.Hidden;

@Hidden
@Controller
public class SpaController {

    @GetMapping({
            "/campaigns", "/campaigns/new", "/campaigns/{id}/edit",
            "/products", "/products/new", "/products/{id}/edit"
    })
    public String forwardToAngular() {
        return "forward:/index.html";
    }
}
