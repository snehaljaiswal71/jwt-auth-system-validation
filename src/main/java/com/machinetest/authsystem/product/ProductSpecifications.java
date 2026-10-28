package com.machinetest.authsystem.product;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> matching(ProductFilter filter) {
        return Specification.allOf(Stream.of(
                        nameContains(filter.name()),
                        categoryEquals(filter.category()),
                        priceAtLeast(filter.minPrice()),
                        priceAtMost(filter.maxPrice()))
                .filter(Objects::nonNull)
                .toList());
    }

    private static Specification<Product> nameContains(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        String pattern = "%" + escapeLike(name.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, '\\');
    }

    private static Specification<Product> categoryEquals(String category) {
        if (!StringUtils.hasText(category)) {
            return null;
        }
        String value = category.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(cb.lower(root.get("category")), value);
    }

    private static Specification<Product> priceAtLeast(BigDecimal minPrice) {
        return minPrice == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    private static Specification<Product> priceAtMost(BigDecimal maxPrice) {
        return maxPrice == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public record ProductFilter(String name, String category, BigDecimal minPrice, BigDecimal maxPrice) {
    }
}
