package com.machinetest.authsystem.product.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PageResponse", description = "One page of results")
public record PageResponse<T>(
        @Schema(description = "Items on this page")
        List<T> content,
        @Schema(description = "Zero based page number", example = "0")
        int page,
        @Schema(description = "Requested page size", example = "10")
        int size,
        @Schema(description = "Total number of matching items", example = "42")
        long totalElements,
        @Schema(description = "Total number of pages", example = "5")
        int totalPages,
        @Schema(description = "Sort applied, e.g. 'price: DESC'", example = "price: DESC")
        String sort,
        @Schema(description = "True when this is the first page")
        boolean isFirst,
        @Schema(description = "True when this is the last page")
        boolean isLast) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.getSort().toString(), page.isFirst(), page.isLast());
    }
}
