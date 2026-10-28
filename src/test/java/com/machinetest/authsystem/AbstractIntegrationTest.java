package com.machinetest.authsystem;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.machinetest.authsystem.geo.Country;
import com.machinetest.authsystem.geo.GeoLocationService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final String PASSWORD = "Secret@123";
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JsonMapper jsonMapper;

    @MockitoBean
    protected GeoLocationService geoLocationService;

    @BeforeEach
    void allowCountryByDefault() {
        when(geoLocationService.resolveCountry(anyString())).thenReturn(Optional.of(new Country("IN", "India")));
    }

    protected static String uniqueUsername() {
        return "test_user_" + SEQUENCE.incrementAndGet() + "_" + System.nanoTime() % 100000;
    }

    protected static String emailFor(String username) {
        return username + "@example.com";
    }

    protected ResultActions signup(String username, String userType) throws Exception {
        return mockMvc.perform(json(post("/api/auth/signup"), Map.of(
                "username", username,
                "email", emailFor(username),
                "password", PASSWORD,
                "userType", userType)));
    }

    protected String signupAndVerify(String username, String userType) throws Exception {
        String body = signup(username, userType).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String otp = jsonMapper.readTree(body).get("otp").asString();
        mockMvc.perform(json(post("/api/auth/verify-otp"), Map.of("username", username, "otp", otp)))
                .andExpect(status().isOk());
        return username;
    }

    protected String login(String username) throws Exception {
        String body = mockMvc.perform(json(post("/api/auth/login"), Map.of("username", username, "password", PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(body).get("accessToken").asString();
    }

    protected String registerAndLogin(String userType) throws Exception {
        return login(signupAndVerify(uniqueUsername(), userType));
    }

    protected MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, Object body) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(body));
    }

    protected MockHttpServletRequestBuilder bearer(MockHttpServletRequestBuilder builder, String token) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    protected JsonNode read(ResultActions actions) throws Exception {
        return jsonMapper.readTree(actions.andReturn().getResponse().getContentAsString());
    }
}
