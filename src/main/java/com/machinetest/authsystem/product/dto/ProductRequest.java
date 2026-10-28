package com.machinetest.authsystem.product.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "ProductRequest", description = "Product details used for create and full update")
public record ProductRequest(
        @Schema(description = "Mandatory field. Product name of 2 to 100 characters; letters, digits, spaces and . , ' & ( ) - _ / only",
                example = "Wireless Mouse", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Name is mandatory")
        @Size(min = 2, max = 100, message = "Name must be 2 to 100 characters")
        @Pattern(regexp = "^[\\p{L}\\p{N} .,'&()_/-]*$", message = "Name contains characters that are not allowed")
        String name,

        @Schema(description = "Optional field. Description of up to 1000 characters; '<' and '>' are not allowed. "
                + "When omitted the product has no description",
                example = "Ergonomic 2.4 GHz wireless mouse", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 1000, message = "Description must be at most 1000 characters")
        @Pattern(regexp = "^[^<>]*$", message = "Description must not contain '<' or '>'")
        String description,

        @Schema(description = "Mandatory field. Unit price between 0.01 and 9999999999.99 with up to 2 decimals",
                example = "799.99", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Price is mandatory")
        @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
        @DecimalMax(value = "9999999999.99", message = "Price must be at most 9999999999.99")
        @Digits(integer = 10, fraction = 2, message = "Price may have at most 2 decimal places")
        BigDecimal price,

        @Schema(description = "Mandatory field. Units in stock between 0 and 1000000", example = "25",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Quantity is mandatory")
        @Min(value = 0, message = "Quantity must not be negative")
        @Max(value = 1_000_000, message = "Quantity must be at most 1000000")
        Integer quantity,

        @Schema(description = "Mandatory field. Category of 2 to 50 characters; letters, digits, spaces, '&', '-' and '_' only",
                example = "Electronics", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Category is mandatory")
        @Size(min = 2, max = 50, message = "Category must be 2 to 50 characters")
        @Pattern(regexp = "^[\\p{L}\\p{N} &_-]*$", message = "Category contains characters that are not allowed")
        String category) {
}
