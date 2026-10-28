package com.machinetest.authsystem.auth;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.machinetest.authsystem.auth.dto.LoginRequest;
import com.machinetest.authsystem.auth.dto.LoginResponse;
import com.machinetest.authsystem.auth.dto.OtpResponse;
import com.machinetest.authsystem.auth.dto.SignupRequest;
import com.machinetest.authsystem.auth.dto.SignupResponse;
import com.machinetest.authsystem.auth.dto.VerifyOtpRequest;
import com.machinetest.authsystem.config.AppProperties;
import com.machinetest.authsystem.exception.ApiException;
import com.machinetest.authsystem.exception.ErrorCode;
import com.machinetest.authsystem.geo.Country;
import com.machinetest.authsystem.geo.CountryRestrictionService;
import com.machinetest.authsystem.otp.OtpService;
import com.machinetest.authsystem.security.AuthenticatedUser;
import com.machinetest.authsystem.security.JwtService;
import com.machinetest.authsystem.security.TokenBlacklist;
import com.machinetest.authsystem.user.InMemoryUserRepository;
import com.machinetest.authsystem.user.User;

@Service
public class AuthService {

    public static final String DASHBOARD_URL = "/api/dashboard";
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final InMemoryUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CountryRestrictionService countryRestrictionService;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final TokenBlacklist tokenBlacklist;
    private final Clock clock;
    private final boolean exposeOtp;
    private final String dummyPasswordHash;

    public AuthService(InMemoryUserRepository userRepository, PasswordEncoder passwordEncoder,
                       CountryRestrictionService countryRestrictionService, OtpService otpService,
                       JwtService jwtService, TokenBlacklist tokenBlacklist, Clock clock,
                       AppProperties appProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.countryRestrictionService = countryRestrictionService;
        this.otpService = otpService;
        this.jwtService = jwtService;
        this.tokenBlacklist = tokenBlacklist;
        this.clock = clock;
        this.exposeOtp = appProperties.otp().exposeInResponse();
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public SignupResponse signup(SignupRequest request, String clientIp) {
        String username = request.username().trim();
        String email = request.email().trim();
        if (userRepository.existsByUsername(username)) {
            throw new ApiException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        Optional<Country> country = countryRestrictionService.assertSignupAllowed(clientIp);
        String countryCode = country.map(Country::code).orElse(null);
        User user = userRepository.insert(new User(UUID.randomUUID(), username, email,
                passwordEncoder.encode(request.password()), request.userType(), countryCode, false, clock.instant()));
        OtpService.IssuedOtp otp = otpService.issue(user.username());
        log.info("User {} registered, awaiting OTP verification", user.id());
        return new SignupResponse(user.username(), user.email(), user.userType(), countryCode,
                "Registration successful. Verify the OTP to activate your account.",
                exposeOtp ? otp.code() : null, otp.expiresAt());
    }

    public void verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (user.isVerified()) {
            throw new ApiException(ErrorCode.ACCOUNT_ALREADY_VERIFIED);
        }
        otpService.verify(user.username(), request.otp());
        userRepository.markVerified(user.username());
        log.info("User {} verified", user.id());
    }

    public OtpResponse resendOtp(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (user.isVerified()) {
            throw new ApiException(ErrorCode.ACCOUNT_ALREADY_VERIFIED);
        }
        OtpService.IssuedOtp otp = otpService.issue(user.username());
        return new OtpResponse("A new OTP has been issued", exposeOtp ? otp.code() : null, otp.expiresAt());
    }

    public Optional<LoginResponse> login(LoginRequest request) {
        Optional<User> user = userRepository.findByUsername(request.username());
        String hash = user.map(User::passwordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        if (user.isEmpty() || !passwordMatches) {
            log.info("Login failed");
            return Optional.empty();
        }
        User authenticated = user.get();
        if (!authenticated.isVerified()) {
            throw new ApiException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }
        JwtService.IssuedToken token = jwtService.generateToken(authenticated);
        log.info("User {} logged in", authenticated.id());
        return Optional.of(new LoginResponse("Bearer", token.token(), token.issuedAt(), token.expiresAt(),
                authenticated.username(), authenticated.userType(), DASHBOARD_URL));
    }

    public String logout(AuthenticatedUser principal) {
        tokenBlacklist.revoke(principal.tokenId(), principal.tokenExpiresAt());
        return principal.username() + " Logged Out";
    }
}
