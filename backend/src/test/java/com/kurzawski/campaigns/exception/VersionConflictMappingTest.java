package com.kurzawski.campaigns.exception;

import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kurzawski.campaigns.controller.CampaignController;
import com.kurzawski.campaigns.model.Campaign;
import com.kurzawski.campaigns.service.CampaignService;

@WebMvcTest(CampaignController.class)
class VersionConflictMappingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CampaignService campaignService;

    @Test
    void versionConflictReturnsConflict() throws Exception {
        doThrow(new ObjectOptimisticLockingFailureException(Campaign.class, 1L))
                .when(campaignService).deleteCampaign(1L);

        mockMvc.perform(delete("/api/campaigns/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").exists());
    }
}
