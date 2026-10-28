package com.machinetest.authsystem.security;

import java.time.Instant;

import com.machinetest.authsystem.user.UserType;

public record AuthenticatedUser(String username, UserType userType, String tokenId, Instant tokenExpiresAt) {
}
