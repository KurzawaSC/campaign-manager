package com.kurzawski.campaigns.dto;

import com.kurzawski.campaigns.model.Town;

public record TownResponse(Long id, String name) {

    public static TownResponse from(Town town) {
        return new TownResponse(town.getId(), town.getName());
    }
}
