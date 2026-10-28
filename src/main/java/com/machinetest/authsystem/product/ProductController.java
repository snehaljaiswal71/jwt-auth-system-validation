package com.machinetest.authsystem.product;

import java.math.BigDecimal;
import java.net.URI;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.machinetest.authsystem.exception.ApiError;
import com.machinetest.authsystem.product.ProductSpecifications.ProductFilter;
import com.machinetest.authsystem.product.dto.PageResponse;
import com.machinetest.authsystem.product.dto.ProductRequest;
import com.machinetest.authsystem.product.dto.ProductResponse;
import com.machinetest.authsystem.security.AuthenticatedUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
@Validated
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Product management. Available to any authenticated user ('Standard User' or 'Administrator')")
@ApiResponse(responseCode = "401", description = "Missing, invalid, expired or logged out token (UNAUTHORISED)",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @Operation(summary = "Create a product", description = "Side effects: inserts a new product row in PostgreSQL "
            + "recording the caller as creator.")
    @ApiResponse(responseCode = "201", description = "Product created; Location header points to the new product")
    @ApiResponse(responseCode = "400", description = "Validation failed (VALIDATION_FAILED, MALFORMED_REQUEST)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request,
                                                  @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        ProductResponse response = productService.create(request, principal.username());
        return ResponseEntity.created(URI.create("/api/products/" + response.id())).body(response);
    }

    @GetMapping
    @Operation(summary = "List products with pagination, sorting and filtering",
            description = "All filters are optional and combined with AND. Sorting uses sort=<property>,<asc|desc> and "
                    + "supports id, name, category, price, quantity, createdAt and updatedAt. Page size defaults to 10 "
                    + "and is capped at 100.")
    @ApiResponse(responseCode = "200", description = "Page of products")
    @ApiResponse(responseCode = "400", description = "Invalid filter, page or sort parameter "
            + "(VALIDATION_FAILED, INVALID_PARAMETER, INVALID_SORT_PROPERTY, INVALID_PRICE_RANGE)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public PageResponse<ProductResponse> list(
            @Parameter(description = "Optional. Case-insensitive partial match on the product name", example = "mouse")
            @RequestParam(required = false) @Size(max = 100) @Pattern(regexp = "^[\\p{L}\\p{N} .,'&()_/-]*$") String name,
            @Parameter(description = "Optional. Case-insensitive exact match on the category", example = "Electronics")
            @RequestParam(required = false) @Size(max = 50) @Pattern(regexp = "^[\\p{L}\\p{N} &_-]*$") String category,
            @Parameter(description = "Optional. Minimum price, inclusive", example = "100")
            @RequestParam(required = false) @DecimalMin("0") BigDecimal minPrice,
            @Parameter(description = "Optional. Maximum price, inclusive", example = "1000")
            @RequestParam(required = false) @DecimalMin("0") BigDecimal maxPrice,
            @Parameter(hidden = true) @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return productService.search(new ProductFilter(name, category, minPrice, maxPrice), pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by id")
    @ApiResponse(responseCode = "200", description = "Product found")
    @ApiResponse(responseCode = "400", description = "Invalid id (INVALID_PARAMETER, VALIDATION_FAILED)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Product not found (PRODUCT_NOT_FOUND)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ProductResponse get(@PathVariable @Min(1) Long id) {
        return productService.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a product", description = "Full update: every mandatory field must be sent; an omitted "
            + "description clears it. Side effects: updates the product row and records the caller as last updater.")
    @ApiResponse(responseCode = "200", description = "Product updated")
    @ApiResponse(responseCode = "400", description = "Validation failed (VALIDATION_FAILED, MALFORMED_REQUEST, INVALID_PARAMETER)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Product not found (PRODUCT_NOT_FOUND)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Product changed concurrently (CONCURRENT_MODIFICATION)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ProductResponse update(@PathVariable @Min(1) Long id, @Valid @RequestBody ProductRequest request,
                                  @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return productService.update(id, request, principal.username());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product", description = "Side effects: permanently deletes the product row.")
    @ApiResponse(responseCode = "204", description = "Product deleted")
    @ApiResponse(responseCode = "400", description = "Invalid id (INVALID_PARAMETER, VALIDATION_FAILED)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Product not found (PRODUCT_NOT_FOUND)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<Void> delete(@PathVariable @Min(1) Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
