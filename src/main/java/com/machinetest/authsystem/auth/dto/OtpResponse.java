package com.machinetest.authsystem.auth.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "OtpResponse", description = "Result of issuing a new OTP")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OtpResponse(
        @Schema(description = "Message for the caller", example = "A new OTP has been issued")
        String message,
        @Schema(description = "One time password. Returned only when app.otp.expose-in-response is enabled", example = "482913")
        String otp,
        @Schema(description = "UTC time at which the OTP expires", example = "2026-10-28T10:20:30Z")
        Instant otpExpiresAt) {
}
