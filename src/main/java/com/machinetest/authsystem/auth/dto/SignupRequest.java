package com.machinetest.authsystem.auth.dto;

import com.machinetest.authsystem.user.UserType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "SignupRequest", description = "Registration details for a new user")
public record SignupRequest(
        @Schema(description = "Mandatory field. Username of 8 to 50 characters; letters, digits, '.', '_' and '-' only",
                example = "john_doe01", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Username is mandatory")
        @Size(min = 8, max = 50, message = "Username must be 8 to 50 characters")
        @Pattern(regexp = "^[A-Za-z0-9._-]*$", message = "Username may contain only letters, digits, '.', '_' and '-'")
        String username,

        @Schema(description = "Mandatory field. Valid email address that must contain both '@' and '_'",
                example = "john_doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Email is mandatory")
        @Size(max = 254, message = "Email must be at most 254 characters")
        @Pattern(regexp = "^(?=[^@]*_)[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$",
                message = "Email must be a valid address containing '@' and '_'")
        String email,

        @Schema(description = "Mandatory field. Password of 8 to 12 characters with at least one uppercase letter, "
                + "one lowercase letter, one digit and one special character; spaces are not allowed",
                example = "Secret@123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Password is mandatory")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{8,12}$",
                message = "Password must be 8 to 12 characters with at least one uppercase letter, one lowercase "
                        + "letter, one digit and one special character, and no spaces")
        String password,

        @Schema(description = "Mandatory field. Allowed values: 'Standard User', 'Administrator'",
                example = "Standard User", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "User type is mandatory and must be 'Standard User' or 'Administrator'")
        UserType userType) {
}
