package com.kurzawski.campaigns.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.kurzawski.campaigns.repository.KeywordRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ProductCampaignRulesTest {

    private static final String PRODUCTS = "/api/products";
    private static final String CAMPAIGNS = "/api/campaigns";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KeywordRepository keywordRepository;

    private Long keywordId;
    private Long productId;
    private BigDecimal startBalance;

    @BeforeEach
    void setUp() throws Exception {
        keywordId = keywordRepository.findAll().get(0).getId();
        productId = createProduct("Rules Mug");
        startBalance = balance();
    }

    @AfterEach
    void cleanUp() throws Exception {
        String content = mockMvc.perform(get(CAMPAIGNS)).andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(content, "$[*].id");
        for (Integer id : ids) {
            mockMvc.perform(delete(CAMPAIGNS + "/" + id)).andExpect(status().isNoContent());
        }
        mockMvc.perform(delete(PRODUCTS + "/" + productId));
    }

    @Test
    void productWithRunningCampaignCannotBeDeleted() throws Exception {
        createCampaign(productId, "ON", "100.00");

        mockMvc.perform(delete(PRODUCTS + "/" + productId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Product \"Rules Mug\" has campaigns. Delete its campaigns first."));

        mockMvc.perform(get(PRODUCTS + "/" + productId)).andExpect(status().isOk());
        assertThat(balance()).isEqualByComparingTo(startBalance.subtract(new BigDecimal("100.00")));
    }

    @Test
    void productWithStoppedCampaignCannotBeDeleted() throws Exception {
        createCampaign(productId, "OFF", "100.00");

        mockMvc.perform(delete(PRODUCTS + "/" + productId)).andExpect(status().isConflict());
    }

    @Test
    void productCanBeDeletedAfterItsCampaignsAreDeleted() throws Exception {
        Long campaign = createCampaign(productId, "ON", "100.00");
        mockMvc.perform(delete(CAMPAIGNS + "/" + campaign)).andExpect(status().isNoContent());

        mockMvc.perform(delete(PRODUCTS + "/" + productId)).andExpect(status().isNoContent());

        assertThat(balance()).isEqualByComparingTo(startBalance);
    }

    @Test
    void movingTheLastCampaignAwayUnblocksDeletion() throws Exception {
        Long otherProduct = createProduct("Other Rules Mug");
        Long campaign = createCampaign(productId, "ON", "100.00");
        mockMvc.perform(put(CAMPAIGNS + "/" + campaign)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(campaignBody(otherProduct, "ON", "100.00")))
                .andExpect(status().isOk());

        mockMvc.perform(delete(PRODUCTS + "/" + productId)).andExpect(status().isNoContent());

        mockMvc.perform(delete(CAMPAIGNS + "/" + campaign)).andExpect(status().isNoContent());
        mockMvc.perform(delete(PRODUCTS + "/" + otherProduct)).andExpect(status().isNoContent());
    }

    @Test
    void renamedProductIsShownInItsCampaigns() throws Exception {
        Long campaign = createCampaign(productId, "ON", "100.00");

        mockMvc.perform(put(PRODUCTS + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed Rules Mug\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(CAMPAIGNS + "/" + campaign))
                .andExpect(jsonPath("$.product.name").value("Renamed Rules Mug"));
    }

    @Test
    void campaignAndProductDeleteRacingNeverBreakTheRule() throws Exception {
        for (int round = 0; round < 200; round++) {
            Long product = createProduct("Race Mug " + round);
            String body = campaignBody(product, "ON", "10.00");
            CountDownLatch start = new CountDownLatch(1);
            Callable<Integer> createCampaign = () -> {
                start.await();
                return mockMvc.perform(post(CAMPAIGNS).contentType(MediaType.APPLICATION_JSON).content(body))
                        .andReturn().getResponse().getStatus();
            };
            Callable<Integer> deleteProduct = () -> {
                start.await();
                return mockMvc.perform(delete(PRODUCTS + "/" + product)).andReturn().getResponse().getStatus();
            };

            Future<Integer> created;
            Future<Integer> deleted;
            try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
                created = executor.submit(createCampaign);
                deleted = executor.submit(deleteProduct);
                start.countDown();
            }

            assertThat(created.get()).isIn(201, 400, 409);
            assertThat(deleted.get()).isIn(204, 409);
            assertThat(created.get() == 201 && deleted.get() == 204).isFalse();

            String content = mockMvc.perform(get(CAMPAIGNS)).andReturn().getResponse().getContentAsString();
            List<Integer> ids = JsonPath.read(content, "$[*].id");
            assertThat(ids).hasSize(created.get() == 201 ? 1 : 0);
            BigDecimal expected = created.get() == 201 ? startBalance.subtract(BigDecimal.TEN) : startBalance;
            assertThat(balance()).isEqualByComparingTo(expected);

            for (Integer id : ids) {
                mockMvc.perform(delete(CAMPAIGNS + "/" + id)).andExpect(status().isNoContent());
            }
            mockMvc.perform(delete(PRODUCTS + "/" + product));
        }
    }

    private Long createProduct(String name) throws Exception {
        String content = mockMvc.perform(post(PRODUCTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(content, "$.id")).longValue();
    }

    private Long createCampaign(Long product, String campaignStatus, String fund) throws Exception {
        ResultActions result = mockMvc.perform(post(CAMPAIGNS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(campaignBody(product, campaignStatus, fund)));
        String content = result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(content, "$.id")).longValue();
    }

    private String campaignBody(Long product, String campaignStatus, String fund) {
        return """
                {"name":"Rules","productId":%d,"keywordIds":[%d],"bidAmount":1,"campaignFund":%s,"status":"%s","radiusKm":5}
                """.formatted(product, keywordId, fund, campaignStatus);
    }

    private BigDecimal balance() throws Exception {
        String content = mockMvc.perform(get("/api/account")).andReturn().getResponse().getContentAsString();
        return new BigDecimal(JsonPath.read(content, "$.balance").toString());
    }
}
