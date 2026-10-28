package com.machinetest.authsystem.dashboard;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.machinetest.authsystem.config.AppProperties;
import com.machinetest.authsystem.exception.ApiError;
import com.machinetest.authsystem.security.AuthenticatedUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/settings")
@Tag(name = "Settings", description = "Administrator only settings")
public class SettingsController {

    private final AppProperties appProperties;

    public SettingsController(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @GetMapping
    @Operation(summary = "View application settings",
            description = "Only users of type 'Administrator' can call this endpoint; 'Standard User' receives 403.")
    @ApiResponse(responseCode = "200", description = "Settings returned")
    @ApiResponse(responseCode = "401", description = "Missing, invalid, expired or logged out token (UNAUTHORISED)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "Caller is not an Administrator (ACCESS_DENIED)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public SettingsResponse settings(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return new SettingsResponse(
                "Settings accessed by Administrator " + principal.username(),
                appProperties.jwt().expiration().toMinutes(),
                appProperties.otp().ttl().toMinutes(),
                appProperties.otp().maxAttempts(),
                appProperties.geo().blockedCountryCodes().stream().sorted().toList());
    }

    @Schema(name = "SettingsResponse", description = "Current security settings")
    public record SettingsResponse(
            @Schema(description = "Message for the caller", example = "Settings accessed by Administrator admin_user01")
            String message,
            @Schema(description = "JWT lifetime in minutes", example = "30")
            long tokenExpiryMinutes,
            @Schema(description = "OTP lifetime in minutes", example = "5")
            long otpExpiryMinutes,
            @Schema(description = "Maximum wrong OTP attempts before the OTP is invalidated", example = "5")
            int otpMaxAttempts,
            @Schema(description = "ISO 3166-1 alpha-2 codes of countries from which sign-up is blocked", example = "[\"AF\", \"IR\", \"SY\"]")
            List<String> blockedCountries) {
    }
}
