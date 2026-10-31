package com.kurzawski.campaigns.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kurzawski.campaigns.dto.ProductRequest;
import com.kurzawski.campaigns.dto.ProductResponse;
import com.kurzawski.campaigns.exception.DuplicateProductNameException;
import com.kurzawski.campaigns.exception.ProductNotFoundException;
import com.kurzawski.campaigns.model.Product;
import com.kurzawski.campaigns.model.Seller;
import com.kurzawski.campaigns.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CurrentSellerProvider currentSellerProvider;

    public ProductService(ProductRepository productRepository, CurrentSellerProvider currentSellerProvider) {
        this.productRepository = productRepository;
        this.currentSellerProvider = currentSellerProvider;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts() {
        Long sellerId = currentSellerProvider.getCurrentSeller().getId();
        return productRepository.findAllBySellerIdOrderByName(sellerId).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        return ProductResponse.from(findOwnedProduct(id, currentSellerProvider.getCurrentSeller()));
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Seller seller = currentSellerProvider.getCurrentSeller();
        if (productRepository.existsBySellerIdAndNormalizedName(seller.getId(), Product.normalizeName(request.name()))) {
            throw new DuplicateProductNameException(request.name());
        }
        try {
            return ProductResponse.from(productRepository.saveAndFlush(new Product(request.name(), seller)));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateProductNameException(request.name());
        }
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Seller seller = currentSellerProvider.getCurrentSeller();
        Product product = findOwnedProduct(id, seller);
        String normalizedName = Product.normalizeName(request.name());
        if (productRepository.existsBySellerIdAndNormalizedNameAndIdNot(seller.getId(), normalizedName, id)) {
            throw new DuplicateProductNameException(request.name());
        }
        product.rename(request.name());
        try {
            return ProductResponse.from(productRepository.saveAndFlush(product));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateProductNameException(request.name());
        }
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = findOwnedProduct(id, currentSellerProvider.getCurrentSeller());
        productRepository.delete(product);
    }

    private Product findOwnedProduct(Long id, Seller seller) {
        return productRepository.findByIdAndSellerId(id, seller.getId())
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
