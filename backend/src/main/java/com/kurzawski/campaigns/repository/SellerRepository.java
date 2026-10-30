package com.kurzawski.campaigns.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kurzawski.campaigns.model.Seller;

public interface SellerRepository extends JpaRepository<Seller, Long> {

    Optional<Seller> findFirstByOrderByIdAsc();
}
