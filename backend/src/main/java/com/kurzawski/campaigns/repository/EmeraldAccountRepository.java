package com.kurzawski.campaigns.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kurzawski.campaigns.model.EmeraldAccount;

public interface EmeraldAccountRepository extends JpaRepository<EmeraldAccount, Long> {

    Optional<EmeraldAccount> findBySellerId(Long sellerId);
}
