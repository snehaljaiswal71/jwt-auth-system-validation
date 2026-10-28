package com.machinetest.authsystem.dashboard;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.machinetest.authsystem.common.MessageResponse;
import com.machinetest.authsystem.exception.ApiError;
import com.machinetest.authsystem.security.AuthenticatedUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Landing endpoint after login")
public class DashboardController {

    @GetMapping
    @Operation(summary = "Dashboard", description = "Any authenticated user ('Standard User' or 'Administrator'). "
            + "Returns 'Welcome <username>'.")
    @ApiResponse(responseCode = "200", description = "Welcome message")
    @ApiResponse(responseCode = "401", description = "Missing, invalid, expired or logged out token (UNAUTHORISED)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public MessageResponse dashboard(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return new MessageResponse("Welcome " + principal.username());
    }
}
