package com.kurzawski.campaigns.model;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;

@Entity
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal bidAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal campaignFund;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status;

    @Column(nullable = false)
    private Integer radiusKm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "town_id")
    private Town town;

    @ManyToMany
    @JoinTable(
            name = "campaign_keyword",
            joinColumns = @JoinColumn(name = "campaign_id"),
            inverseJoinColumns = @JoinColumn(name = "keyword_id"))
    private Set<Keyword> keywords = new HashSet<>();

    protected Campaign() {
    }

    public Campaign(Seller seller) {
        this.seller = seller;
    }

    public void update(
            String name,
            BigDecimal bidAmount,
            BigDecimal campaignFund,
            CampaignStatus status,
            Integer radiusKm,
            Product product,
            Town town,
            Set<Keyword> keywords) {
        this.name = name;
        this.bidAmount = bidAmount;
        this.campaignFund = campaignFund;
        this.status = status;
        this.radiusKm = radiusKm;
        this.product = product;
        this.town = town;
        this.keywords = new HashSet<>(keywords);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getBidAmount() {
        return bidAmount;
    }

    public BigDecimal getCampaignFund() {
        return campaignFund;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public Integer getRadiusKm() {
        return radiusKm;
    }

    public Product getProduct() {
        return product;
    }

    public Town getTown() {
        return town;
    }

    public Set<Keyword> getKeywords() {
        return keywords;
    }
}
