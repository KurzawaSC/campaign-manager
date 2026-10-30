package com.kurzawski.campaigns.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kurzawski.campaigns.model.Town;

public interface TownRepository extends JpaRepository<Town, Long> {

    List<Town> findAllByOrderByName();
}
