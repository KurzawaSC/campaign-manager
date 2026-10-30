package com.kurzawski.campaigns.dto;

import com.kurzawski.campaigns.model.Product;

public record ProductResponse(Long id, String name) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName());
    }
}
