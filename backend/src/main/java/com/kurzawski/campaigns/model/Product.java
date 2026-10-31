package com.kurzawski.campaigns.model;

import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"seller_id", "normalized_name"}))
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    protected Product() {
    }

    public Product(String name, Seller seller) {
        this.seller = seller;
        rename(name);
    }

    public static String normalizeName(String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }

    public void rename(String name) {
        this.name = name.strip();
        this.normalizedName = normalizeName(name);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
