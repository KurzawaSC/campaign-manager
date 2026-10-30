package com.kurzawski.campaigns.service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kurzawski.campaigns.dto.CampaignRequest;
import com.kurzawski.campaigns.dto.CampaignResponse;
import com.kurzawski.campaigns.exception.CampaignNotFoundException;
import com.kurzawski.campaigns.exception.InvalidReferenceException;
import com.kurzawski.campaigns.model.Campaign;
import com.kurzawski.campaigns.model.Keyword;
import com.kurzawski.campaigns.model.Product;
import com.kurzawski.campaigns.model.Seller;
import com.kurzawski.campaigns.model.Town;
import com.kurzawski.campaigns.repository.CampaignRepository;
import com.kurzawski.campaigns.repository.KeywordRepository;
import com.kurzawski.campaigns.repository.ProductRepository;
import com.kurzawski.campaigns.repository.TownRepository;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final ProductRepository productRepository;
    private final KeywordRepository keywordRepository;
    private final TownRepository townRepository;
    private final AccountService accountService;
    private final CurrentSellerProvider currentSellerProvider;

    public CampaignService(
            CampaignRepository campaignRepository,
            ProductRepository productRepository,
            KeywordRepository keywordRepository,
            TownRepository townRepository,
            AccountService accountService,
            CurrentSellerProvider currentSellerProvider) {
        this.campaignRepository = campaignRepository;
        this.productRepository = productRepository;
        this.keywordRepository = keywordRepository;
        this.townRepository = townRepository;
        this.accountService = accountService;
        this.currentSellerProvider = currentSellerProvider;
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> getCampaigns(Long productId) {
        Long sellerId = currentSellerProvider.getCurrentSeller().getId();
        List<Campaign> campaigns = productId == null
                ? campaignRepository.findAllBySellerIdOrderById(sellerId)
                : campaignRepository.findAllBySellerIdAndProductIdOrderById(sellerId, productId);
        return campaigns.stream().map(CampaignResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CampaignResponse getCampaign(Long id) {
        return CampaignResponse.from(findOwnedCampaign(id, currentSellerProvider.getCurrentSeller()));
    }

    @Transactional
    public CampaignResponse createCampaign(CampaignRequest request) {
        Seller seller = currentSellerProvider.getCurrentSeller();
        Campaign campaign = new Campaign(seller);
        applyRequest(campaign, request, seller);
        accountService.withdraw(campaign.getCampaignFund());
        return CampaignResponse.from(campaignRepository.save(campaign));
    }

    @Transactional
    public CampaignResponse updateCampaign(Long id, CampaignRequest request) {
        Seller seller = currentSellerProvider.getCurrentSeller();
        Campaign campaign = findOwnedCampaign(id, seller);
        BigDecimal previousFund = campaign.getCampaignFund();
        applyRequest(campaign, request, seller);
        settleFundChange(previousFund, campaign.getCampaignFund());
        return CampaignResponse.from(campaign);
    }

    @Transactional
    public void deleteCampaign(Long id) {
        Seller seller = currentSellerProvider.getCurrentSeller();
        Campaign campaign = findOwnedCampaign(id, seller);
        accountService.deposit(campaign.getCampaignFund());
        campaignRepository.delete(campaign);
    }

    private void settleFundChange(BigDecimal previousFund, BigDecimal newFund) {
        BigDecimal difference = newFund.subtract(previousFund);
        if (difference.signum() > 0) {
            accountService.withdraw(difference);
        } else if (difference.signum() < 0) {
            accountService.deposit(difference.negate());
        }
    }

    private Campaign findOwnedCampaign(Long id, Seller seller) {
        return campaignRepository.findByIdAndSellerId(id, seller.getId())
                .orElseThrow(() -> new CampaignNotFoundException(id));
    }

    private void applyRequest(Campaign campaign, CampaignRequest request, Seller seller) {
        campaign.update(
                request.name().strip(),
                request.bidAmount(),
                request.campaignFund(),
                request.status(),
                request.radiusKm(),
                findProduct(request.productId(), seller),
                findTown(request.townId()),
                findKeywords(request.keywordIds()));
    }

    private Product findProduct(Long productId, Seller seller) {
        return productRepository.findByIdAndSellerId(productId, seller.getId())
                .orElseThrow(() -> new InvalidReferenceException("productId", "Product " + productId + " not found"));
    }

    private Town findTown(Long townId) {
        if (townId == null) {
            return null;
        }
        return townRepository.findById(townId)
                .orElseThrow(() -> new InvalidReferenceException("townId", "Town " + townId + " not found"));
    }

    private Set<Keyword> findKeywords(Set<Long> keywordIds) {
        Set<Keyword> keywords = new HashSet<>(keywordRepository.findAllById(keywordIds));
        if (keywords.size() != keywordIds.size()) {
            throw new InvalidReferenceException("keywordIds", "Some keywords were not found");
        }
        return keywords;
    }
}
