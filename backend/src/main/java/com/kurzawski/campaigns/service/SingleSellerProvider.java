package com.kurzawski.campaigns.service;

import org.springframework.stereotype.Component;

import com.kurzawski.campaigns.model.Seller;
import com.kurzawski.campaigns.repository.SellerRepository;

@Component
public class SingleSellerProvider implements CurrentSellerProvider {

    private final SellerRepository sellerRepository;

    public SingleSellerProvider(SellerRepository sellerRepository) {
        this.sellerRepository = sellerRepository;
    }

    @Override
    public Seller getCurrentSeller() {
        return sellerRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("No seller found"));
    }
}
