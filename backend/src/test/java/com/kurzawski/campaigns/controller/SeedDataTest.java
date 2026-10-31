package com.kurzawski.campaigns.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.kurzawski.campaigns.model.Keyword;
import com.kurzawski.campaigns.repository.KeywordRepository;

@SpringBootTest
@AutoConfigureMockMvc
class SeedDataTest {

    private static final List<String> VOIVODESHIP_CAPITALS = List.of(
            "Bialystok", "Bydgoszcz", "Gdansk", "Gorzow Wielkopolski", "Katowice", "Kielce",
            "Krakow", "Lodz", "Lublin", "Olsztyn", "Opole", "Poznan",
            "Rzeszow", "Szczecin", "Torun", "Warsaw", "Wroclaw", "Zielona Gora");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KeywordRepository keywordRepository;

    @Test
    void townsAreTheEighteenVoivodeshipCapitals() throws Exception {
        List<String> towns = namesFrom("/api/towns");

        assertThat(towns).hasSize(18).containsExactlyInAnyOrderElementsOf(VOIVODESHIP_CAPITALS);
    }

    @Test
    void productsAreChristmasCampaignProducts() throws Exception {
        assertThat(namesFrom("/api/products")).contains("Coca-Cola", "LEGO Bricks", "Christmas Tree", "Advent Calendar");
    }

    @Test
    void everyProductHasAMatchingKeyword() throws Exception {
        List<String> keywords = keywordRepository.findAll().stream().map(Keyword::getName).toList();

        assertThat(keywords).hasSizeGreaterThan(400);
        assertThat(namesFrom("/api/products").stream().map(name -> name.toLowerCase(Locale.ROOT)).toList())
                .allSatisfy(product -> assertThat(keywords).contains(product));
    }

    @Test
    void keywordSuggestionsAreLimitedToTwenty() throws Exception {
        mockMvc.perform(get("/api/keywords").param("query", "christmas")).andExpect(jsonPath("$", hasSize(20)));
    }

    private List<String> namesFrom(String path) throws Exception {
        String content = mockMvc.perform(get(path)).andReturn().getResponse().getContentAsString();
        return JsonPath.read(content, "$[*].name");
    }
}
