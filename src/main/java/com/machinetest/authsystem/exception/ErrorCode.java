package com.machinetest.authsystem.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "Username is already registered"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email is already registered"),
    COUNTRY_RESTRICTED(HttpStatus.FORBIDDEN, "Sign-up is not allowed from your country"),
    COUNTRY_UNRESOLVED(HttpStatus.SERVICE_UNAVAILABLE, "Unable to determine your country, please try again later"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    ACCOUNT_ALREADY_VERIFIED(HttpStatus.CONFLICT, "Account is already verified"),
    ACCOUNT_NOT_VERIFIED(HttpStatus.FORBIDDEN, "Account is not verified, please verify the OTP first"),
    OTP_NOT_FOUND(HttpStatus.BAD_REQUEST, "No active OTP found, please request a new one"),
    OTP_EXPIRED(HttpStatus.BAD_REQUEST, "OTP has expired, please request a new one"),
    OTP_INVALID(HttpStatus.BAD_REQUEST, "OTP is invalid"),
    OTP_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Too many invalid OTP attempts, please request a new one"),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Product not found"),
    INVALID_SORT_PROPERTY(HttpStatus.BAD_REQUEST, "Sorting is supported only by id, name, category, price, quantity, createdAt and updatedAt"),
    INVALID_PRICE_RANGE(HttpStatus.BAD_REQUEST, "minPrice must be less than or equal to maxPrice");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
