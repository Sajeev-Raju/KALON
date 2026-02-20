package com.kalon.exception;

public class AddressLimitException extends RuntimeException {
    public AddressLimitException(String message) {
        super(message);
    }
}
