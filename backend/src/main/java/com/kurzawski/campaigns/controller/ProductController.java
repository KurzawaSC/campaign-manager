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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.kurzawski.campaigns.dto.ProductRequest;
import com.kurzawski.campaigns.dto.ProductResponse;
import com.kurzawski.campaigns.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Create, edit, delete and list the seller's products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "List the seller's products")
    public List<ProductResponse> getProducts() {
        return productService.getProducts();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product")
    @ApiResponse(responseCode = "200", description = "Product found")
    @ApiResponse(responseCode = "404", description = "Product not found")
    public ProductResponse getProduct(@PathVariable Long id) {
        return productService.getProduct(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a product")
    @ApiResponse(responseCode = "201", description = "Product created")
    @ApiResponse(responseCode = "400", description = "Invalid data, details in fieldErrors")
    @ApiResponse(responseCode = "409", description = "A product with this name already exists")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.createProduct(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(product.id())
                .toUri();
        return ResponseEntity.created(location).body(product);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a product")
    @ApiResponse(responseCode = "200", description = "Product updated")
    @ApiResponse(responseCode = "400", description = "Invalid data, details in fieldErrors")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @ApiResponse(responseCode = "409", description = "A product with this name already exists")
    public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a product", description = "A product with any campaign, ON or OFF, cannot be deleted.")
    @ApiResponse(responseCode = "204", description = "Product deleted")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @ApiResponse(responseCode = "409", description = "The product has campaigns")
    public void deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
    }
}
