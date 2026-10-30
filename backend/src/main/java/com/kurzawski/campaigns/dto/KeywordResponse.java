package com.kurzawski.campaigns.dto;

import com.kurzawski.campaigns.model.Keyword;

public record KeywordResponse(Long id, String name) {

    public static KeywordResponse from(Keyword keyword) {
        return new KeywordResponse(keyword.getId(), keyword.getName());
    }
}
