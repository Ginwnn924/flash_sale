package com.flashsale.exception;

public class SoldOutException extends RuntimeException {

    public SoldOutException() {
        super("Product is sold out");
    }

    public SoldOutException(String message) {
        super(message);
    }
}
