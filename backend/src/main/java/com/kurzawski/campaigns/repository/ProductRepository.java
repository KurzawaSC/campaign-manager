package com.kurzawski.campaigns.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.kurzawski.campaigns.model.Product;

import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllBySellerIdOrderByName(Long sellerId);

    Optional<Product> findByIdAndSellerId(Long id, Long sellerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Product> findForUpdateByIdAndSellerId(Long id, Long sellerId);

    boolean existsBySellerIdAndNormalizedName(Long sellerId, String normalizedName);

    boolean existsBySellerIdAndNormalizedNameAndIdNot(Long sellerId, String normalizedName, Long id);
}
