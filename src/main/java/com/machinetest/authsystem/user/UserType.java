package com.machinetest.authsystem.user;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserType", description = "Type of user. Allowed values: 'Standard User', 'Administrator'",
        allowableValues = {"Standard User", "Administrator"})
public enum UserType {

    STANDARD_USER("Standard User"),
    ADMINISTRATOR("Administrator");

    private final String displayName;

    UserType(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public String getAuthority() {
        return "ROLE_" + name();
    }

    @JsonCreator
    public static UserType fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalised = value.trim();
        return Arrays.stream(values())
                .filter(type -> type.displayName.equalsIgnoreCase(normalised) || type.name().equalsIgnoreCase(normalised))
                .findFirst()
                .orElse(null);
    }
}
