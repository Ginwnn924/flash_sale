package com.flashsale.exception;

public class DuplicateOrderException extends RuntimeException {

    public DuplicateOrderException() {
        super("User already purchased this flash sale item");
    }

    public DuplicateOrderException(String message) {
        super(message);
    }
}
