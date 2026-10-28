package com.machinetest.authsystem.product;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.machinetest.authsystem.exception.ApiException;
import com.machinetest.authsystem.exception.ErrorCode;
import com.machinetest.authsystem.product.ProductSpecifications.ProductFilter;
import com.machinetest.authsystem.product.dto.PageResponse;
import com.machinetest.authsystem.product.dto.ProductRequest;
import com.machinetest.authsystem.product.dto.ProductResponse;

@Service
public class ProductService {

    private static final Set<String> SORTABLE_PROPERTIES =
            Set.of("id", "name", "category", "price", "quantity", "createdAt", "updatedAt");

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResponse create(ProductRequest request, String username) {
        Product product = new Product(request.name().trim(), normalise(request.description()), request.price(),
                request.quantity(), request.category().trim(), username);
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return ProductResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(ProductFilter filter, Pageable pageable) {
        if (filter.minPrice() != null && filter.maxPrice() != null && filter.minPrice().compareTo(filter.maxPrice()) > 0) {
            throw new ApiException(ErrorCode.INVALID_PRICE_RANGE);
        }
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE_PROPERTIES.contains(order.getProperty())) {
                throw new ApiException(ErrorCode.INVALID_SORT_PROPERTY);
            }
        }
        Page<Product> page = productRepository.findAll(ProductSpecifications.matching(filter), pageable);
        return PageResponse.from(page, ProductResponse::from);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request, String username) {
        Product product = find(id);
        product.update(request.name().trim(), normalise(request.description()), request.price(), request.quantity(),
                request.category().trim(), username);
        productRepository.flush();
        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(Long id) {
        productRepository.delete(find(id));
    }

    private Product find(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private static String normalise(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
