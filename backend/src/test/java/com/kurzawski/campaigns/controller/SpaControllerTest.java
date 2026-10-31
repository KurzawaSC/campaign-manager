package com.kurzawski.campaigns.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SpaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {
            "/campaigns", "/campaigns/new", "/campaigns/1/edit",
            "/products", "/products/new", "/products/1/edit"
    })
    void angularRoutesForwardToIndex(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void apiRoutesStillReturnJson() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    void unknownPathsReturnNotFound() throws Exception {
        mockMvc.perform(get("/does-not-exist")).andExpect(status().isNotFound());
    }

    @Test
    void angularRoutesAreHiddenFromSwagger() throws Exception {
        String docs = mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();

        assertThat(docs).contains("\"/api/products\"").doesNotContain("\"/products\"", "\"/campaigns\"");
    }
}
