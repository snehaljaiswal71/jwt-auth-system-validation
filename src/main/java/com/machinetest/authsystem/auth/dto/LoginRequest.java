package com.machinetest.authsystem.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "LoginRequest", description = "Login credentials")
public record LoginRequest(
        @Schema(description = "Mandatory field. Registered username", example = "john_doe01",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Username is mandatory")
        @Size(max = 50, message = "Username must be at most 50 characters")
        String username,

        @Schema(description = "Mandatory field. Account password", example = "Secret@123",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Password is mandatory")
        @Size(max = 12, message = "Password must be at most 12 characters")
        String password) {
}
