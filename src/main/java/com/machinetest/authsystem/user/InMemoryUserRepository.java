package com.machinetest.authsystem.user;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.machinetest.authsystem.exception.ApiException;
import com.machinetest.authsystem.exception.ErrorCode;

@Repository
public class InMemoryUserRepository {

    private final Map<String, User> usersByUsername = new ConcurrentHashMap<>();
    private final Map<String, String> usernamesByEmail = new ConcurrentHashMap<>();

    public synchronized User insert(User user) {
        String usernameKey = key(user.username());
        String emailKey = key(user.email());
        if (usersByUsername.containsKey(usernameKey)) {
            throw new ApiException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (usernamesByEmail.containsKey(emailKey)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        usersByUsername.put(usernameKey, user);
        usernamesByEmail.put(emailKey, usernameKey);
        return user;
    }

    public User markVerified(String username) {
        User updated = usersByUsername.computeIfPresent(key(username), (k, existing) -> existing.markVerified());
        if (updated == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        return updated;
    }

    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(key(username)));
    }

    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(usernamesByEmail.get(key(email))).map(usersByUsername::get);
    }

    public boolean existsByUsername(String username) {
        return usersByUsername.containsKey(key(username));
    }

    public boolean existsByEmail(String email) {
        return usernamesByEmail.containsKey(key(email));
    }

    private static String key(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
