package com.machinetest.authsystem.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TokenBlacklist {

    private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();
    private final Clock clock;

    public TokenBlacklist(Clock clock) {
        this.clock = clock;
    }

    public void revoke(String tokenId, Instant expiresAt) {
        revokedTokens.put(tokenId, expiresAt);
    }

    public boolean isRevoked(String tokenId) {
        return revokedTokens.containsKey(tokenId);
    }

    @Scheduled(fixedDelayString = "PT1M")
    public void purgeExpired() {
        Instant now = clock.instant();
        revokedTokens.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    }
}
