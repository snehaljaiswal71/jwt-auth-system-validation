package com.machinetest.authsystem.user;

import java.time.Instant;
import java.util.UUID;

public record User(
        UUID id,
        String username,
        String email,
        String passwordHash,
        UserType userType,
        String countryCode,
        boolean isVerified,
        Instant createdAt) {

    public User markVerified() {
        return new User(id, username, email, passwordHash, userType, countryCode, true, createdAt);
    }
}
