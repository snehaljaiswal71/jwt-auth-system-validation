package com.machinetest.authsystem.config;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Otp otp, Geo geo, Cors cors) {

    public record Jwt(String secret, Duration expiration, String issuer) {
    }

    public record Otp(int length, Duration ttl, int maxAttempts, boolean exposeInResponse) {
    }

    public record Geo(String lookupUrl, Duration timeout, boolean failOpen, boolean trustForwardedHeader,
                      List<String> blockedCountries) {

        public Set<String> blockedCountryCodes() {
            return blockedCountries == null ? Set.of() : blockedCountries.stream()
                    .map(code -> code.trim().toUpperCase())
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
