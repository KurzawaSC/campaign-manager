package com.kurzawski.campaigns.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.kurzawski.campaigns.model.Campaign;
import com.kurzawski.campaigns.model.CampaignStatus;
import com.kurzawski.campaigns.model.Keyword;
import com.kurzawski.campaigns.model.Product;
import com.kurzawski.campaigns.model.Seller;
import com.kurzawski.campaigns.repository.CampaignRepository;
import com.kurzawski.campaigns.repository.KeywordRepository;
import com.kurzawski.campaigns.repository.ProductRepository;
import com.kurzawski.campaigns.repository.SellerRepository;
import com.kurzawski.campaigns.repository.TownRepository;
import com.kurzawski.campaigns.service.CurrentSellerProvider;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class CampaignApiTest {

    private static final String CAMPAIGNS = "/api/campaigns";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CurrentSellerProvider currentSellerProvider;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private KeywordRepository keywordRepository;

    @Autowired
    private TownRepository townRepository;

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    private Long productId;
    private Long otherProductId;
    private Long keywordId;
    private Long townId;
    private BigDecimal startBalance;

    @BeforeEach
    void setUp() throws Exception {
        List<Product> products = productRepository.findAllBySellerIdOrderByName(
                currentSellerProvider.getCurrentSeller().getId());
        productId = products.get(0).getId();
        otherProductId = products.get(1).getId();
        keywordId = keywordRepository.findAll().get(0).getId();
        townId = townRepository.findAll().get(0).getId();
        startBalance = balance();
    }

    @AfterEach
    void deleteCampaigns() throws Exception {
        String content = mockMvc.perform(get(CAMPAIGNS)).andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(content, "$[*].id");
        for (Integer id : ids) {
            mockMvc.perform(delete(CAMPAIGNS + "/" + id)).andExpect(status().isNoContent());
        }
    }

    @Test
    void createReturnsCampaignAndDeductsFund() throws Exception {
        ResultActions result = create(body("100.50", "ON", productId)).andExpect(status().isCreated());

        result.andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Summer sale"))
                .andExpect(jsonPath("$.product.id").value(productId))
                .andExpect(jsonPath("$.keywords", hasSize(1)))
                .andExpect(jsonPath("$.bidAmount").value(1.50))
                .andExpect(jsonPath("$.campaignFund").value(100.50))
                .andExpect(jsonPath("$.status").value("ON"))
                .andExpect(jsonPath("$.town.id").value(townId))
                .andExpect(jsonPath("$.radiusKm").value(25));
        assertBalanceChangedBy("-100.50");
    }

    @Test
    void raisingFundDeductsOnlyTheDifference() throws Exception {
        Long id = createCampaign("100.00");

        update(id, body("150.00", "ON", productId)).andExpect(status().isOk());

        assertBalanceChangedBy("-150.00");
    }

    @Test
    void loweringFundRefundsTheDifference() throws Exception {
        Long id = createCampaign("100.00");

        update(id, body("70.00", "ON", productId)).andExpect(status().isOk());

        assertBalanceChangedBy("-70.00");
    }

    @Test
    void changingStatusKeepsBalance() throws Exception {
        Long id = createCampaign("100.00");

        update(id, body("100.00", "OFF", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OFF"));

        assertBalanceChangedBy("-100.00");
    }

    @Test
    void deleteRefundsWholeFund() throws Exception {
        Long id = createCampaign("100.00");

        mockMvc.perform(delete(CAMPAIGNS + "/" + id)).andExpect(status().isNoContent());

        assertBalanceChangedBy("0");
        mockMvc.perform(get(CAMPAIGNS + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void createAboveBalanceFailsAndSavesNothing() throws Exception {
        create(body(startBalance.add(new BigDecimal("0.01")).toPlainString(), "ON", productId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").exists());

        assertBalanceChangedBy("0");
        mockMvc.perform(get(CAMPAIGNS)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void updateAboveBalanceFailsAndKeepsCampaign() throws Exception {
        Long id = createCampaign("100.00");
        String tooMuch = startBalance.add(new BigDecimal("1")).toPlainString();

        update(id, body(tooMuch, "OFF", otherProductId)).andExpect(status().isConflict());

        assertBalanceChangedBy("-100.00");
        mockMvc.perform(get(CAMPAIGNS + "/" + id))
                .andExpect(jsonPath("$.campaignFund").value(100.00))
                .andExpect(jsonPath("$.status").value("ON"))
                .andExpect(jsonPath("$.product.id").value(productId));
    }

    @Test
    void fundCanUseWholeBalance() throws Exception {
        create(body(startBalance.toPlainString(), "ON", productId)).andExpect(status().isCreated());

        assertThat(balance()).isEqualByComparingTo("0");
    }

    @Test
    void missingRequiredFieldsAreReportedPerField() throws Exception {
        create("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.productId").exists())
                .andExpect(jsonPath("$.fieldErrors.keywordIds").exists())
                .andExpect(jsonPath("$.fieldErrors.bidAmount").exists())
                .andExpect(jsonPath("$.fieldErrors.campaignFund").exists())
                .andExpect(jsonPath("$.fieldErrors.status").exists())
                .andExpect(jsonPath("$.fieldErrors.radiusKm").exists())
                .andExpect(jsonPath("$.fieldErrors.townId").doesNotExist());
        assertBalanceChangedBy("0");
    }

    @Test
    void validationMessagesAreInEnglish() throws Exception {
        mockMvc.perform(post(CAMPAIGNS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "pl-PL")
                        .content("{}"))
                .andExpect(jsonPath("$.fieldErrors.name").value("must not be blank"));
    }

    @Test
    void bidBelowMinimumIsRejected() throws Exception {
        create(body("0.09", "100.00", "ON", productId, keywordId, 25))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.bidAmount").value("must be at least 0.10"));
    }

    @Test
    void bidAtMinimumIsAccepted() throws Exception {
        create(body("0.10", "100.00", "ON", productId, keywordId, 25)).andExpect(status().isCreated());
    }

    @Test
    void nonPositiveFundAndRadiusAreRejected() throws Exception {
        create(body("1.00", "0", "ON", productId, keywordId, 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.campaignFund").exists())
                .andExpect(jsonPath("$.fieldErrors.radiusKm").exists());
    }

    @Test
    void amountsWithTooManyDecimalsAreRejected() throws Exception {
        create(body("1.001", "1.001", "ON", productId, keywordId, 25))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.bidAmount").exists())
                .andExpect(jsonPath("$.fieldErrors.campaignFund").exists());
    }

    @Test
    void townIsOptional() throws Exception {
        create("""
                {"name":"No town","productId":%d,"keywordIds":[%d],"bidAmount":1,"campaignFund":1,"status":"ON","radiusKm":5}
                """.formatted(productId, keywordId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.town").isEmpty());
    }

    @Test
    void unknownReferencesAreRejectedPerField() throws Exception {
        create(body("1.00", "1.00", "ON", -1L, keywordId, 25))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.productId").exists());
        create(body("1.00", "1.00", "ON", productId, -1L, 25))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.keywordIds").exists());
        assertBalanceChangedBy("0");
    }

    @Test
    void invalidStatusValueIsRejected() throws Exception {
        create(body("1.00", "MAYBE", productId)).andExpect(status().isBadRequest());
    }

    @Test
    void missingCampaignReturnsNotFound() throws Exception {
        mockMvc.perform(get(CAMPAIGNS + "/-1")).andExpect(status().isNotFound());
        update(-1L, body("1.00", "ON", productId)).andExpect(status().isNotFound());
        mockMvc.perform(delete(CAMPAIGNS + "/-1")).andExpect(status().isNotFound());
    }

    @Test
    void listCanBeFilteredByProduct() throws Exception {
        Long first = createCampaign("1.00");
        create(body("1.00", "ON", otherProductId)).andExpect(status().isCreated());

        mockMvc.perform(get(CAMPAIGNS)).andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get(CAMPAIGNS).param("productId", productId.toString()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(first));
    }

    @Test
    void anotherSellersDataIsNotAccessible() throws Exception {
        Seller otherSeller = sellerRepository.save(new Seller("Other Seller"));
        Product foreignProduct = productRepository.save(new Product("Foreign product", otherSeller));
        Campaign foreignCampaign = new Campaign(otherSeller);
        Keyword keyword = keywordRepository.findById(keywordId).orElseThrow();
        foreignCampaign.update(
                "Foreign campaign",
                BigDecimal.ONE,
                BigDecimal.TEN,
                CampaignStatus.ON,
                5,
                foreignProduct,
                null,
                Set.of(keyword));
        foreignCampaign = campaignRepository.save(foreignCampaign);
        String path = CAMPAIGNS + "/" + foreignCampaign.getId();

        try {
            mockMvc.perform(get(path)).andExpect(status().isNotFound());
            update(foreignCampaign.getId(), body("1.00", "ON", productId)).andExpect(status().isNotFound());
            mockMvc.perform(delete(path)).andExpect(status().isNotFound());
            mockMvc.perform(get(CAMPAIGNS)).andExpect(jsonPath("$", hasSize(0)));
            create(body("1.00", "ON", foreignProduct.getId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.productId").exists());
            assertBalanceChangedBy("0");
        } finally {
            campaignRepository.delete(foreignCampaign);
            productRepository.delete(foreignProduct);
            sellerRepository.delete(otherSeller);
        }
    }

    @Test
    void readEndpointsReturnSeedData() throws Exception {
        mockMvc.perform(get("/api/products")).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").exists());
        mockMvc.perform(get("/api/towns")).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").exists());
        mockMvc.perform(get("/api/config")).andExpect(jsonPath("$.minBidAmount").value(0.10));
        mockMvc.perform(get("/api/keywords").param("query", "WIRE"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("wireless"));
    }

    private Long createCampaign(String fund) throws Exception {
        String content = create(body(fund, "ON", productId))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(content, "$.id")).longValue();
    }

    private ResultActions create(String body) throws Exception {
        return mockMvc.perform(post(CAMPAIGNS).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions update(Long id, String body) throws Exception {
        return mockMvc.perform(put(CAMPAIGNS + "/" + id).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String body(String fund, String status, Long product) {
        return body("1.50", fund, status, product, keywordId, 25);
    }

    private String body(String bid, String fund, String status, Long product, Long keyword, int radius) {
        return """
                {"name":"Summer sale","productId":%d,"keywordIds":[%d],"bidAmount":%s,"campaignFund":%s,"status":"%s","townId":%d,"radiusKm":%d}
                """.formatted(product, keyword, bid, fund, status, townId, radius);
    }

    private BigDecimal balance() throws Exception {
        String content = mockMvc.perform(get("/api/account")).andReturn().getResponse().getContentAsString();
        return new BigDecimal(JsonPath.read(content, "$.balance").toString());
    }

    private void assertBalanceChangedBy(String change) throws Exception {
        assertThat(balance()).isEqualByComparingTo(startBalance.add(new BigDecimal(change)));
    }
}
