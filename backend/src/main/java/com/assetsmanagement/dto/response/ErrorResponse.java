package com.assetsmanagement.dto.response;

import java.util.List;

public record ErrorResponse(
        int status,
        String error,
        String message,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {}

    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, null);
    }
}
