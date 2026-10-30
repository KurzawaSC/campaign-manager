package com.kurzawski.campaigns.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.kurzawski.campaigns.model.EmeraldAccount;
import com.kurzawski.campaigns.repository.EmeraldAccountRepository;
import com.kurzawski.campaigns.repository.KeywordRepository;
import com.kurzawski.campaigns.repository.ProductRepository;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class CampaignConcurrencyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CurrentSellerProvider currentSellerProvider;

    @Autowired
    private EmeraldAccountRepository accountRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private KeywordRepository keywordRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void staleAccountUpdateIsRejected() {
        Long sellerId = currentSellerProvider.getCurrentSeller().getId();
        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        TransactionTemplate inner = new TransactionTemplate(transactionManager);
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        BigDecimal balanceBefore = accountRepository.findBySellerId(sellerId).orElseThrow().getBalance();

        assertThatThrownBy(() -> outer.executeWithoutResult(status -> {
            EmeraldAccount stale = accountRepository.findBySellerId(sellerId).orElseThrow();
            inner.executeWithoutResult(innerStatus ->
                    accountRepository.findBySellerId(sellerId).orElseThrow().deposit(BigDecimal.ONE));
            stale.deposit(BigDecimal.TEN);
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        BigDecimal balanceAfter = accountRepository.findBySellerId(sellerId).orElseThrow().getBalance();
        assertThat(balanceAfter).isEqualByComparingTo(balanceBefore.add(BigDecimal.ONE));
        inner.executeWithoutResult(status ->
                accountRepository.findBySellerId(sellerId).orElseThrow().withdraw(BigDecimal.ONE));
    }

    @Test
    void parallelCreatesNeverLoseOrInventMoney() throws Exception {
        Long sellerId = currentSellerProvider.getCurrentSeller().getId();
        Long productId = productRepository.findAllBySellerIdOrderByName(sellerId).get(0).getId();
        Long keywordId = keywordRepository.findAll().get(0).getId();
        BigDecimal startBalance = accountRepository.findBySellerId(sellerId).orElseThrow().getBalance();
        BigDecimal fund = new BigDecimal("1500.00");
        String body = """
                {"name":"Parallel","productId":%d,"keywordIds":[%d],"bidAmount":1,"campaignFund":%s,"status":"ON","radiusKm":5}
                """.formatted(productId, keywordId, fund);

        int requests = 10;
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> create = () -> {
            start.await();
            return mockMvc.perform(post("/api/campaigns").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        };

        List<Future<Integer>> futures = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(requests)) {
            for (int i = 0; i < requests; i++) {
                futures.add(executor.submit(create));
            }
            start.countDown();
        }

        int created = 0;
        for (Future<Integer> future : futures) {
            int status = future.get();
            assertThat(status).isIn(201, 409);
            if (status == 201) {
                created++;
            }
        }

        BigDecimal balance = accountRepository.findBySellerId(sellerId).orElseThrow().getBalance();
        String content = mockMvc.perform(get("/api/campaigns")).andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(content, "$[*].id");

        assertThat(created).isGreaterThan(0);
        assertThat(ids).hasSize(created);
        assertThat(balance).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(balance).isEqualByComparingTo(startBalance.subtract(fund.multiply(BigDecimal.valueOf(created))));

        for (Integer id : ids) {
            mockMvc.perform(delete("/api/campaigns/" + id));
        }
        assertThat(accountRepository.findBySellerId(sellerId).orElseThrow().getBalance())
                .isEqualByComparingTo(startBalance);
    }
}
