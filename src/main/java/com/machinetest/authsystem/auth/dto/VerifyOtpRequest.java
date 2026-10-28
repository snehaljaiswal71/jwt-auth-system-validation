package com.machinetest.authsystem.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "VerifyOtpRequest", description = "OTP verification details")
public record VerifyOtpRequest(
        @Schema(description = "Mandatory field. Username used during sign-up", example = "john_doe01",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Username is mandatory")
        @Size(min = 8, max = 50, message = "Username must be 8 to 50 characters")
        String username,

        @Schema(description = "Mandatory field. Six digit OTP issued at sign-up or resend", example = "482913",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "OTP is mandatory")
        @Pattern(regexp = "^\\d{6}$", message = "OTP must be exactly 6 digits")
        String otp) {
}
