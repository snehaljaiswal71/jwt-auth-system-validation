package com.machinetest.authsystem.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ResendOtpRequest", description = "Request for a new OTP")
public record ResendOtpRequest(
        @Schema(description = "Mandatory field. Username used during sign-up", example = "john_doe01",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Username is mandatory")
        @Size(min = 8, max = 50, message = "Username must be 8 to 50 characters")
        String username) {
}
