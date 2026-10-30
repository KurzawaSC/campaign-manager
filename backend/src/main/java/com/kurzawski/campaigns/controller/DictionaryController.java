package com.kurzawski.campaigns.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kurzawski.campaigns.dto.KeywordResponse;
import com.kurzawski.campaigns.dto.TownResponse;
import com.kurzawski.campaigns.service.DictionaryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Dictionaries", description = "Pre-populated keywords and towns")
public class DictionaryController {

    private final DictionaryService dictionaryService;

    public DictionaryController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @GetMapping("/keywords")
    @Operation(summary = "Suggest keywords", description = "Returns up to 20 keywords containing the query, ignoring case.")
    public List<KeywordResponse> findKeywords(
            @Parameter(description = "Text typed by the user") @RequestParam(defaultValue = "") String query) {
        return dictionaryService.findKeywords(query);
    }

    @GetMapping("/towns")
    @Operation(summary = "List towns")
    public List<TownResponse> getTowns() {
        return dictionaryService.getTowns();
    }
}
