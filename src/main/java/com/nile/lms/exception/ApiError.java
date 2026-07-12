package com.nile.lms.exception;

import java.time.Instant;

public record ApiError(
        int status,
        String code,
        String message,
        String timestamp
) {
    public static ApiError of(int status, String code, String message) {
        return new ApiError(status, code, message, Instant.now().toString());
    }
}