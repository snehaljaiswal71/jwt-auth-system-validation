package com.machinetest.authsystem;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.machinetest.authsystem.geo.Country;
import com.machinetest.authsystem.user.InMemoryUserRepository;

class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private InMemoryUserRepository userRepository;

    @Test
    @DisplayName("Sign-up stores a BCrypt hash and returns an OTP for verification")
    void test_signup_stores_hashed_password_and_returns_otp() throws Exception {
        String username = uniqueUsername();

        signup(username, "Standard User")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.userType").value("Standard User"))
                .andExpect(jsonPath("$.countryCode").value("IN"))
                .andExpect(jsonPath("$.otp").isString())
                .andExpect(jsonPath("$.password").doesNotExist());

        String storedHash = userRepository.findByUsername(username).orElseThrow().passwordHash();
        assertNotEquals(PASSWORD, storedHash, "Password must never be stored in plain text");
    }

    @Test
    @DisplayName("Sign-up rejects every field that breaks the validation rules")
    void test_signup_rejects_invalid_fields() throws Exception {
        mockMvc.perform(json(post("/api/auth/signup"), Map.of(
                        "username", "short",
                        "email", "johndoe@example.com",
                        "password", "password1",
                        "userType", "Guest")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.username").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.userType").exists());
    }

    @Test
    @DisplayName("Password longer than 12 characters is rejected")
    void test_signup_rejects_password_longer_than_twelve() throws Exception {
        String username = uniqueUsername();
        mockMvc.perform(json(post("/api/auth/signup"), Map.of(
                        "username", username,
                        "email", emailFor(username),
                        "password", "Secret@123456",
                        "userType", "Administrator")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    @DisplayName("Sign-up from a restricted country is forbidden")
    void test_signup_from_restricted_country_is_forbidden() throws Exception {
        when(geoLocationService.resolveCountry(anyString())).thenReturn(Optional.of(new Country("IR", "Iran")));

        signup(uniqueUsername(), "Standard User")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COUNTRY_RESTRICTED"));
    }

    @Test
    @DisplayName("Sign-up fails closed when the country cannot be resolved")
    void test_signup_fails_when_country_unresolved() throws Exception {
        when(geoLocationService.resolveCountry(anyString())).thenReturn(Optional.empty());

        signup(uniqueUsername(), "Standard User")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("COUNTRY_UNRESOLVED"));
    }

    @Test
    @DisplayName("Duplicate username is rejected with 409")
    void test_signup_duplicate_username_conflict() throws Exception {
        String username = uniqueUsername();
        signup(username, "Standard User").andExpect(status().isCreated());

        signup(username, "Standard User")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("Login before OTP verification is forbidden")
    void test_login_before_verification_is_forbidden() throws Exception {
        String username = uniqueUsername();
        signup(username, "Standard User").andExpect(status().isCreated());

        mockMvc.perform(json(post("/api/auth/login"), Map.of("username", username, "password", PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_VERIFIED"));
    }

    @Test
    @DisplayName("Wrong OTP is rejected and the account stays unverified")
    void test_wrong_otp_is_rejected() throws Exception {
        String username = uniqueUsername();
        String otp = read(signup(username, "Standard User")).get("otp").asString();
        String wrongOtp = otp.equals("000000") ? "111111" : "000000";

        mockMvc.perform(json(post("/api/auth/verify-otp"), Map.of("username", username, "otp", wrongOtp)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OTP_INVALID"));
    }

    @Test
    @DisplayName("Invalid login redirects to the error endpoint which returns 'Login Failed'")
    void test_invalid_login_redirects_to_login_failed() throws Exception {
        String username = signupAndVerify(uniqueUsername(), "Standard User");

        mockMvc.perform(json(post("/api/auth/login"), Map.of("username", username, "password", "Wrong@1234")))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/api/auth/error"));

        mockMvc.perform(get("/api/auth/error"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Login Failed"));
    }

    @Test
    @DisplayName("Successful login returns a JWT and the dashboard welcomes the user")
    void test_login_then_dashboard_welcomes_user() throws Exception {
        String username = signupAndVerify(uniqueUsername(), "Standard User");
        String token = login(username);

        mockMvc.perform(bearer(get("/api/dashboard"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Welcome " + username));
    }

    @Test
    @DisplayName("Dashboard without a token is unauthorised")
    void test_dashboard_without_token_is_unauthorised() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORISED"));
    }

    @Test
    @DisplayName("Tampered token is rejected")
    void test_tampered_token_is_rejected() throws Exception {
        String token = registerAndLogin("Standard User");
        String tampered = token.substring(0, token.length() - 4) + (token.endsWith("AAAA") ? "BBBB" : "AAAA");

        mockMvc.perform(bearer(get("/api/dashboard"), tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token is invalid or expired"));
    }

    @Test
    @DisplayName("Only Administrator can access settings")
    void test_settings_requires_administrator() throws Exception {
        String standardToken = registerAndLogin("Standard User");
        String adminToken = registerAndLogin("Administrator");

        mockMvc.perform(bearer(get("/api/settings"), standardToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(bearer(get("/api/settings"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Administrator")))
                .andExpect(jsonPath("$.blockedCountries", not(org.hamcrest.Matchers.empty())));
    }

    @Test
    @DisplayName("Logout returns '<username> Logged Out' and revokes the token everywhere")
    void test_logout_revokes_token() throws Exception {
        String username = signupAndVerify(uniqueUsername(), "Administrator");
        String token = login(username);

        mockMvc.perform(bearer(post("/api/auth/logout"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(username + " Logged Out"));

        mockMvc.perform(bearer(get("/api/dashboard"), token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token has been logged out"));
        mockMvc.perform(bearer(get("/api/settings"), token))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(bearer(get("/api/products"), token))
                .andExpect(status().isUnauthorized());
    }
}
