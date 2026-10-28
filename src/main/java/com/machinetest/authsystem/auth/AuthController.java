package com.machinetest.authsystem.auth;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.machinetest.authsystem.auth.dto.LoginRequest;
import com.machinetest.authsystem.auth.dto.LoginResponse;
import com.machinetest.authsystem.auth.dto.OtpResponse;
import com.machinetest.authsystem.auth.dto.ResendOtpRequest;
import com.machinetest.authsystem.auth.dto.SignupRequest;
import com.machinetest.authsystem.auth.dto.SignupResponse;
import com.machinetest.authsystem.auth.dto.VerifyOtpRequest;
import com.machinetest.authsystem.common.MessageResponse;
import com.machinetest.authsystem.exception.ApiError;
import com.machinetest.authsystem.geo.ClientIpResolver;
import com.machinetest.authsystem.security.AuthenticatedUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Sign-up, OTP verification, login and logout")
public class AuthController {

    public static final String LOGIN_ERROR_URL = "/api/auth/error";
    public static final String LOGIN_FAILED_MESSAGE = "Login Failed";

    private final AuthService authService;
    private final ClientIpResolver clientIpResolver;

    public AuthController(AuthService authService, ClientIpResolver clientIpResolver) {
        this.authService = authService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/signup")
    @SecurityRequirements
    @Operation(summary = "Register a new user",
            description = "Public endpoint. Validates the details, detects the caller's country from their IP address "
                    + "and rejects sign-ups from restricted countries. Side effects: stores the user in memory as "
                    + "unverified with a BCrypt hashed password and issues a 6 digit OTP valid for 5 minutes.")
    @ApiResponse(responseCode = "201", description = "User registered, OTP issued")
    @ApiResponse(responseCode = "400", description = "Validation failed (VALIDATION_FAILED, MALFORMED_REQUEST)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "Sign-up from a restricted country (COUNTRY_RESTRICTED)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Username or email already registered (USERNAME_ALREADY_EXISTS, EMAIL_ALREADY_EXISTS)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "503", description = "Country could not be determined (COUNTRY_UNRESOLVED)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request, HttpServletRequest httpRequest) {
        SignupResponse response = authService.signup(request, clientIpResolver.resolve(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify-otp")
    @SecurityRequirements
    @Operation(summary = "Verify the OTP and activate the account",
            description = "Public endpoint. Side effects: marks the user as verified and invalidates the OTP. "
                    + "After 5 wrong attempts the OTP is invalidated and a new one must be requested.")
    @ApiResponse(responseCode = "200", description = "Account verified")
    @ApiResponse(responseCode = "400", description = "Validation failed or OTP invalid, expired or missing "
            + "(VALIDATION_FAILED, OTP_INVALID, OTP_EXPIRED, OTP_NOT_FOUND)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "User not found (USER_NOT_FOUND)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Account already verified (ACCOUNT_ALREADY_VERIFIED)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "429", description = "Too many wrong attempts (OTP_ATTEMPTS_EXCEEDED)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public MessageResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request);
        return new MessageResponse("Account verified successfully. You can now log in.");
    }

    @PostMapping("/resend-otp")
    @SecurityRequirements
    @Operation(summary = "Issue a new OTP",
            description = "Public endpoint. Side effects: replaces any existing OTP for the user with a new one.")
    @ApiResponse(responseCode = "200", description = "New OTP issued")
    @ApiResponse(responseCode = "400", description = "Validation failed (VALIDATION_FAILED)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "User not found (USER_NOT_FOUND)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Account already verified (ACCOUNT_ALREADY_VERIFIED)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public OtpResponse resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        return authService.resendOtp(request.username());
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Log in and receive a JWT",
            description = "Public endpoint. On success returns a signed JWT to call the dashboard and other "
                    + "protected endpoints. On invalid credentials responds with 303 See Other redirecting to "
                    + LOGIN_ERROR_URL + ", which returns the 'Login Failed' message.")
    @ApiResponse(responseCode = "200", description = "Login successful")
    @ApiResponse(responseCode = "303", description = "Invalid username or password, redirect to the error endpoint")
    @ApiResponse(responseCode = "400", description = "Validation failed (VALIDATION_FAILED)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "Account not verified yet (ACCOUNT_NOT_VERIFIED)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create(LOGIN_ERROR_URL)).build());
    }

    @GetMapping("/error")
    @SecurityRequirements
    @Operation(summary = "Login error endpoint",
            description = "Public endpoint. Target of the redirect after an invalid login; always returns 'Login Failed'.")
    @ApiResponse(responseCode = "401", description = "Login Failed")
    public ResponseEntity<MessageResponse> loginError() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse(LOGIN_FAILED_MESSAGE));
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out",
            description = "Any authenticated user. Side effects: revokes the presented JWT so it can no longer be "
                    + "used for the dashboard or any other endpoint.")
    @ApiResponse(responseCode = "200", description = "'<username> Logged Out'")
    @ApiResponse(responseCode = "401", description = "Missing, invalid, expired or already logged out token (UNAUTHORISED)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public MessageResponse logout(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return new MessageResponse(authService.logout(principal));
    }
}
