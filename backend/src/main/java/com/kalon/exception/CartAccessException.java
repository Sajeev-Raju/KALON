package com.kalon.exception;

public class CartAccessException extends RuntimeException {
    public CartAccessException(String message) {
        super(message);
    }
}
