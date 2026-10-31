package com.kurzawski.campaigns.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

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
import com.kurzawski.campaigns.model.Product;
import com.kurzawski.campaigns.model.Seller;
import com.kurzawski.campaigns.repository.ProductRepository;
import com.kurzawski.campaigns.repository.SellerRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ProductApiTest {

    private static final String PRODUCTS = "/api/products";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SellerRepository sellerRepository;

    private Set<Long> seedProductIds;

    @BeforeEach
    void rememberSeedProducts() {
        seedProductIds = productRepository.findAll().stream().map(Product::getId).collect(Collectors.toSet());
    }

    @AfterEach
    void deleteCreatedProducts() {
        productRepository.findAll().stream()
                .filter(product -> !seedProductIds.contains(product.getId()))
                .forEach(productRepository::delete);
    }

    @Test
    void createReturnsTrimmedProductWithLocation() throws Exception {
        create("  Travel Mug  ")
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Travel Mug"));
    }

    @Test
    void createdProductAppearsInListAndDetails() throws Exception {
        Long id = createProduct("Travel Mug");

        mockMvc.perform(get(PRODUCTS + "/" + id)).andExpect(jsonPath("$.name").value("Travel Mug"));
        String list = mockMvc.perform(get(PRODUCTS)).andReturn().getResponse().getContentAsString();
        List<String> names = JsonPath.read(list, "$[*].name");
        assertThat(names).contains("Travel Mug").isSorted();
    }

    @Test
    void editChangesTheName() throws Exception {
        Long id = createProduct("Travel Mug");

        update(id, "Thermal Mug").andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Thermal Mug"));

        mockMvc.perform(get(PRODUCTS + "/" + id)).andExpect(jsonPath("$.name").value("Thermal Mug"));
    }

    @Test
    void deleteRemovesTheProduct() throws Exception {
        Long id = createProduct("Travel Mug");

        mockMvc.perform(delete(PRODUCTS + "/" + id)).andExpect(status().isNoContent());

        mockMvc.perform(get(PRODUCTS + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void invalidNamesAreRejected() throws Exception {
        create(" ").andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.name").exists());
        create("x".repeat(256)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.name").exists());
        mockMvc.perform(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void duplicateNameIsRejectedIgnoringCaseAndSpaces() throws Exception {
        createProduct("Travel Mug");

        create(" TRAVEL mug ")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors.name").value("A product named \"TRAVEL mug\" already exists"));
    }

    @Test
    void editCannotTakeAnotherProductsName() throws Exception {
        createProduct("Travel Mug");
        Long other = createProduct("Thermal Mug");

        update(other, "travel mug").andExpect(status().isConflict()).andExpect(jsonPath("$.fieldErrors.name").exists());

        mockMvc.perform(get(PRODUCTS + "/" + other)).andExpect(jsonPath("$.name").value("Thermal Mug"));
    }

    @Test
    void editCanChangeTheCaseOfItsOwnName() throws Exception {
        Long id = createProduct("Travel Mug");

        update(id, "TRAVEL MUG").andExpect(status().isOk()).andExpect(jsonPath("$.name").value("TRAVEL MUG"));
    }

    @Test
    void parallelCreatesWithTheSameNameLeaveOneProduct() throws Exception {
        int requests = 8;
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> createSame = () -> {
            start.await();
            return create("Parallel Mug").andReturn().getResponse().getStatus();
        };

        List<Future<Integer>> futures = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(requests)) {
            for (int i = 0; i < requests; i++) {
                futures.add(executor.submit(createSame));
            }
            start.countDown();
        }

        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> future : futures) {
            statuses.add(future.get());
        }
        assertThat(statuses).containsOnly(201, 409);
        assertThat(statuses).containsOnlyOnce(201);
    }

    @Test
    void missingProductReturnsNotFound() throws Exception {
        mockMvc.perform(get(PRODUCTS + "/-1")).andExpect(status().isNotFound());
        update(-1L, "Anything").andExpect(status().isNotFound());
        mockMvc.perform(delete(PRODUCTS + "/-1")).andExpect(status().isNotFound());
    }

    @Test
    void anotherSellersProductIsNotAccessibleAndItsNameIsFree() throws Exception {
        Seller otherSeller = sellerRepository.save(new Seller("Other Seller"));
        Product foreign = productRepository.save(new Product("Foreign Mug", otherSeller));
        String path = PRODUCTS + "/" + foreign.getId();

        try {
            mockMvc.perform(get(path)).andExpect(status().isNotFound());
            update(foreign.getId(), "Renamed").andExpect(status().isNotFound());
            mockMvc.perform(delete(path)).andExpect(status().isNotFound());
            create("Foreign Mug").andExpect(status().isCreated());
        } finally {
            productRepository.delete(foreign);
            sellerRepository.delete(otherSeller);
        }
    }

    private Long createProduct(String name) throws Exception {
        String content = create(name).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(content, "$.id")).longValue();
    }

    private ResultActions create(String name) throws Exception {
        return mockMvc.perform(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON).content(body(name)));
    }

    private ResultActions update(Long id, String name) throws Exception {
        return mockMvc.perform(put(PRODUCTS + "/" + id).contentType(MediaType.APPLICATION_JSON).content(body(name)));
    }

    private String body(String name) {
        return "{\"name\":\"" + name + "\"}";
    }
}
