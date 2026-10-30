package com.kurzawski.campaigns.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kurzawski.campaigns.model.Keyword;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {

    List<Keyword> findTop20ByNameContainingIgnoreCaseOrderByName(String query);
}
