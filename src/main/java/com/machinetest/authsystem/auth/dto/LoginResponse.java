package com.machinetest.authsystem.auth.dto;

import java.time.Instant;

import com.machinetest.authsystem.user.UserType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginResponse", description = "JWT issued after a successful login")
public record LoginResponse(
        @Schema(description = "Token type to use in the Authorization header", example = "Bearer")
        String tokenType,
        @Schema(description = "Signed JWT access token")
        String accessToken,
        @Schema(description = "UTC time at which the token was issued", example = "2026-10-28T10:15:30Z")
        Instant issuedAt,
        @Schema(description = "UTC time at which the token expires", example = "2026-10-28T10:45:30Z")
        Instant expiresAt,
        @Schema(description = "Logged in username", example = "john_doe01")
        String username,
        @Schema(description = "User type. Allowed values: 'Standard User', 'Administrator'", example = "Standard User")
        UserType userType,
        @Schema(description = "Dashboard endpoint to call with the token", example = "/api/dashboard")
        String dashboardUrl) {
}
