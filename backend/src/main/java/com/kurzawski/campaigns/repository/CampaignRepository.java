package com.kurzawski.campaigns.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.kurzawski.campaigns.model.Campaign;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    @EntityGraph(attributePaths = {"product", "town", "keywords"})
    List<Campaign> findAllBySellerIdOrderById(Long sellerId);

    @EntityGraph(attributePaths = {"product", "town", "keywords"})
    List<Campaign> findAllBySellerIdAndProductIdOrderById(Long sellerId, Long productId);

    @EntityGraph(attributePaths = {"product", "town", "keywords"})
    Optional<Campaign> findByIdAndSellerId(Long id, Long sellerId);
}
