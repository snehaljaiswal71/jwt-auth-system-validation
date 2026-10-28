package com.machinetest.authsystem.exception;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiError", description = "Error payload returned for every failed request")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        @Schema(description = "UTC time of the error in ISO 8601 format", example = "2026-10-28T10:15:30Z")
        Instant timestamp,
        @Schema(description = "HTTP status code", example = "400")
        int status,
        @Schema(description = "Machine readable error code", example = "VALIDATION_FAILED")
        String code,
        @Schema(description = "Human readable error message", example = "Request validation failed")
        String message,
        @Schema(description = "Request path that produced the error", example = "/api/auth/signup")
        String path,
        @Schema(description = "Field level validation errors keyed by field name")
        Map<String, String> fieldErrors) {

    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(Instant.now(), status, code, message, path, Map.of());
    }

    public static ApiError of(int status, String code, String message, String path, Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), status, code, message, path, fieldErrors);
    }
}
