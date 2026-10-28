package com.machinetest.authsystem;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductIntegrationTest extends AbstractIntegrationTest {

    private Map<String, Object> product(String name, String category, String price, int quantity) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("description", "test description");
        body.put("price", price);
        body.put("quantity", quantity);
        body.put("category", category);
        return body;
    }

    private long create(String token, Map<String, Object> body) throws Exception {
        return read(mockMvc.perform(json(bearer(post("/api/products"), token), body)).andExpect(status().isCreated()))
                .get("id").asLong();
    }

    @Test
    @DisplayName("Product endpoints require authentication")
    void test_products_require_authentication() throws Exception {
        mockMvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
        mockMvc.perform(json(post("/api/products"), product("test_product", "test_cat", "10.00", 1)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Create, read, update and delete a product")
    void test_product_crud() throws Exception {
        String token = registerAndLogin("Standard User");

        long id = read(mockMvc.perform(json(bearer(post("/api/products"), token), product("Wireless Mouse", "Electronics", "799.99", 25)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Wireless Mouse"))).get("id").asLong();

        mockMvc.perform(bearer(get("/api/products/" + id), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(799.99));

        mockMvc.perform(json(bearer(put("/api/products/" + id), token), product("Wireless Mouse Pro", "Electronics", "999.00", 10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Wireless Mouse Pro"))
                .andExpect(jsonPath("$.quantity").value(10));

        mockMvc.perform(bearer(delete("/api/products/" + id), token)).andExpect(status().isNoContent());
        mockMvc.perform(bearer(get("/api/products/" + id), token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("Invalid product payload is rejected with field errors")
    void test_create_product_validation() throws Exception {
        String token = registerAndLogin("Standard User");
        Map<String, Object> body = product("<script>", "Electronics", "-1", -5);

        mockMvc.perform(json(bearer(post("/api/products"), token), body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.price").exists())
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());
    }

    @Test
    @DisplayName("List supports filtering, sorting and pagination")
    void test_list_filter_sort_paginate() throws Exception {
        String token = registerAndLogin("Administrator");
        String category = "testcat" + System.nanoTime() % 100000;
        create(token, product("test_alpha", category, "10.00", 1));
        create(token, product("test_beta", category, "20.00", 1));
        create(token, product("test_gamma", category, "30.00", 1));

        mockMvc.perform(bearer(get("/api/products"), token)
                        .param("category", category)
                        .param("minPrice", "15")
                        .param("sort", "price,desc")
                        .param("size", "1")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("test_gamma"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @DisplayName("Unsupported sort property and inverted price range are rejected")
    void test_list_rejects_invalid_parameters() throws Exception {
        String token = registerAndLogin("Standard User");

        mockMvc.perform(bearer(get("/api/products"), token).param("sort", "createdBy,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SORT_PROPERTY"));

        mockMvc.perform(bearer(get("/api/products"), token).param("minPrice", "50").param("maxPrice", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PRICE_RANGE"));
    }
}
