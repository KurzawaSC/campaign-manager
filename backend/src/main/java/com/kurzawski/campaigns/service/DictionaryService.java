package com.kurzawski.campaigns.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kurzawski.campaigns.dto.KeywordResponse;
import com.kurzawski.campaigns.dto.TownResponse;
import com.kurzawski.campaigns.repository.KeywordRepository;
import com.kurzawski.campaigns.repository.TownRepository;

@Service
public class DictionaryService {

    private final KeywordRepository keywordRepository;
    private final TownRepository townRepository;

    public DictionaryService(KeywordRepository keywordRepository, TownRepository townRepository) {
        this.keywordRepository = keywordRepository;
        this.townRepository = townRepository;
    }

    @Transactional(readOnly = true)
    public List<KeywordResponse> findKeywords(String query) {
        return keywordRepository.findTop20ByNameContainingIgnoreCaseOrderByName(query.strip()).stream()
                .map(KeywordResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TownResponse> getTowns() {
        return townRepository.findAllByOrderByName().stream()
                .map(TownResponse::from)
                .toList();
    }
}
