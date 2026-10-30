package com.kurzawski.campaigns.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.kurzawski.campaigns.dto.CampaignRequest;
import com.kurzawski.campaigns.dto.CampaignResponse;
import com.kurzawski.campaigns.service.CampaignService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/campaigns")
@Tag(name = "Campaigns", description = "Create, edit, delete and list campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping
    @Operation(summary = "List campaigns", description = "Returns the seller's campaigns, optionally for one product.")
    public List<CampaignResponse> getCampaigns(
            @Parameter(description = "Return only campaigns of this product") @RequestParam(required = false) Long productId) {
        return campaignService.getCampaigns(productId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a campaign")
    @ApiResponse(responseCode = "200", description = "Campaign found")
    @ApiResponse(responseCode = "404", description = "Campaign not found")
    public CampaignResponse getCampaign(@PathVariable Long id) {
        return campaignService.getCampaign(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a campaign", description = "Deducts the campaign fund from the Emerald account.")
    @ApiResponse(responseCode = "201", description = "Campaign created")
    @ApiResponse(responseCode = "400", description = "Invalid data, details in fieldErrors")
    @ApiResponse(responseCode = "409", description = "Insufficient funds or concurrent change")
    public ResponseEntity<CampaignResponse> createCampaign(@Valid @RequestBody CampaignRequest request) {
        CampaignResponse campaign = campaignService.createCampaign(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(campaign.id())
                .toUri();
        return ResponseEntity.created(location).body(campaign);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Edit a campaign",
            description = "Deducts the difference when the fund grows and refunds it when the fund shrinks.")
    @ApiResponse(responseCode = "200", description = "Campaign updated")
    @ApiResponse(responseCode = "400", description = "Invalid data, details in fieldErrors")
    @ApiResponse(responseCode = "404", description = "Campaign not found")
    @ApiResponse(responseCode = "409", description = "Insufficient funds or concurrent change")
    public CampaignResponse updateCampaign(@PathVariable Long id, @Valid @RequestBody CampaignRequest request) {
        return campaignService.updateCampaign(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a campaign", description = "Refunds the whole campaign fund to the Emerald account.")
    @ApiResponse(responseCode = "204", description = "Campaign deleted")
    @ApiResponse(responseCode = "404", description = "Campaign not found")
    @ApiResponse(responseCode = "409", description = "Concurrent change")
    public void deleteCampaign(@PathVariable Long id) {
        campaignService.deleteCampaign(id);
    }
}
