package com.machinetest.authsystem.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MessageResponse", description = "Simple message payload")
public record MessageResponse(
        @Schema(description = "Message for the caller", example = "Welcome john_doe1")
        String message) {
}
