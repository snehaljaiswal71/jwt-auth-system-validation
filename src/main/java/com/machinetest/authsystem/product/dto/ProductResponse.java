package com.machinetest.authsystem.product.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.machinetest.authsystem.product.Product;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProductResponse", description = "Product details")
public record ProductResponse(
        @Schema(description = "Product id", example = "1")
        Long id,
        @Schema(description = "Product name", example = "Wireless Mouse")
        String name,
        @Schema(description = "Product description; null when not provided", example = "Ergonomic 2.4 GHz wireless mouse")
        String description,
        @Schema(description = "Unit price", example = "799.99")
        BigDecimal price,
        @Schema(description = "Units in stock", example = "25")
        int quantity,
        @Schema(description = "Category", example = "Electronics")
        String category,
        @Schema(description = "Username of the creator", example = "john_doe01")
        String createdBy,
        @Schema(description = "Username of the last updater", example = "john_doe01")
        String updatedBy,
        @Schema(description = "UTC creation time", example = "2026-10-28T10:15:30Z")
        Instant createdAt,
        @Schema(description = "UTC time of the last update", example = "2026-10-28T10:15:30Z")
        Instant updatedAt) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getPrice(),
                product.getQuantity(), product.getCategory(), product.getCreatedBy(), product.getUpdatedBy(),
                product.getCreatedAt(), product.getUpdatedAt());
    }
}
