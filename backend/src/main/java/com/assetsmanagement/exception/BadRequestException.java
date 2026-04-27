package com.assetsmanagement.exception;

/**
 * Thrown for invalid client requests (validation failures, duplicate data, etc.).
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
