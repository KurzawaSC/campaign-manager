package com.kurzawski.campaigns.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kurzawski.campaigns.dto.ProductResponse;
import com.kurzawski.campaigns.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CurrentSellerProvider currentSellerProvider;

    public ProductService(ProductRepository productRepository, CurrentSellerProvider currentSellerProvider) {
        this.productRepository = productRepository;
        this.currentSellerProvider = currentSellerProvider;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts() {
        Long sellerId = currentSellerProvider.getCurrentSeller().getId();
        return productRepository.findAllBySellerIdOrderByName(sellerId).stream()
                .map(ProductResponse::from)
                .toList();
    }
}
