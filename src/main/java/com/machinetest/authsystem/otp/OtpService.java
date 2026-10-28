package com.machinetest.authsystem.otp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.machinetest.authsystem.config.AppProperties;
import com.machinetest.authsystem.exception.ApiException;
import com.machinetest.authsystem.exception.ErrorCode;

@Service
public class OtpService {

    private final SecureRandom random = new SecureRandom();
    private final Map<String, OtpEntry> otps = new ConcurrentHashMap<>();
    private final AppProperties.Otp properties;
    private final Clock clock;

    public OtpService(AppProperties appProperties, Clock clock) {
        this.properties = appProperties.otp();
        this.clock = clock;
    }

    public IssuedOtp issue(String username) {
        StringBuilder code = new StringBuilder(properties.length());
        for (int i = 0; i < properties.length(); i++) {
            code.append(random.nextInt(10));
        }
        Instant expiresAt = clock.instant().plus(properties.ttl());
        otps.put(key(username), new OtpEntry(hash(code.toString()), expiresAt, 0));
        return new IssuedOtp(code.toString(), expiresAt);
    }

    public void verify(String username, String code) {
        String key = key(username);
        OtpEntry entry = otps.get(key);
        if (entry == null) {
            throw new ApiException(ErrorCode.OTP_NOT_FOUND);
        }
        if (clock.instant().isAfter(entry.expiresAt())) {
            otps.remove(key, entry);
            throw new ApiException(ErrorCode.OTP_EXPIRED);
        }
        if (entry.failedAttempts() >= properties.maxAttempts()) {
            otps.remove(key, entry);
            throw new ApiException(ErrorCode.OTP_ATTEMPTS_EXCEEDED);
        }
        if (!MessageDigest.isEqual(entry.codeHash(), hash(code))) {
            OtpEntry failed = entry.withFailedAttempt();
            if (failed.failedAttempts() >= properties.maxAttempts()) {
                otps.remove(key, entry);
                throw new ApiException(ErrorCode.OTP_ATTEMPTS_EXCEEDED);
            }
            otps.replace(key, entry, failed);
            throw new ApiException(ErrorCode.OTP_INVALID);
        }
        if (!otps.remove(key, entry)) {
            throw new ApiException(ErrorCode.OTP_NOT_FOUND);
        }
    }

    @Scheduled(fixedDelayString = "PT1M")
    public void purgeExpired() {
        Instant now = clock.instant();
        otps.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
    }

    private static String key(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private static byte[] hash(String code) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public record IssuedOtp(String code, Instant expiresAt) {
    }

    private record OtpEntry(byte[] codeHash, Instant expiresAt, int failedAttempts) {

        OtpEntry withFailedAttempt() {
            return new OtpEntry(codeHash, expiresAt, failedAttempts + 1);
        }
    }
}
