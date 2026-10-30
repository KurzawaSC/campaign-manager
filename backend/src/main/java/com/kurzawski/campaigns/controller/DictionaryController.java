package com.kurzawski.campaigns.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kurzawski.campaigns.dto.KeywordResponse;
import com.kurzawski.campaigns.dto.TownResponse;
import com.kurzawski.campaigns.service.DictionaryService;

@RestController
@RequestMapping("/api")
public class DictionaryController {

    private final DictionaryService dictionaryService;

    public DictionaryController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @GetMapping("/keywords")
    public List<KeywordResponse> findKeywords(@RequestParam(defaultValue = "") String query) {
        return dictionaryService.findKeywords(query);
    }

    @GetMapping("/towns")
    public List<TownResponse> getTowns() {
        return dictionaryService.getTowns();
    }
}
