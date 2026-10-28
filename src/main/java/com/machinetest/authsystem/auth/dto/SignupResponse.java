package com.machinetest.authsystem.auth.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.machinetest.authsystem.user.UserType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SignupResponse", description = "Result of a successful registration")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SignupResponse(
        @Schema(description = "Registered username", example = "john_doe01")
        String username,
        @Schema(description = "Registered email", example = "john_doe@example.com")
        String email,
        @Schema(description = "User type. Allowed values: 'Standard User', 'Administrator'", example = "Standard User")
        UserType userType,
        @Schema(description = "ISO 3166-1 alpha-2 country code detected from the caller's location; omitted when it could not be resolved",
                example = "IN")
        String countryCode,
        @Schema(description = "Next step for the user", example = "Registration successful. Verify the OTP to activate your account.")
        String message,
        @Schema(description = "One time password for verification. Returned only when app.otp.expose-in-response is enabled "
                + "because no email gateway is configured", example = "482913")
        String otp,
        @Schema(description = "UTC time at which the OTP expires", example = "2026-10-28T10:20:30Z")
        Instant otpExpiresAt) {
}
