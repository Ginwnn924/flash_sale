package com.flashsale.exception;

public class FlashSaleNotActiveException extends RuntimeException {

    public FlashSaleNotActiveException() {
        super("Flash sale is not active");
    }

    public FlashSaleNotActiveException(String message) {
        super(message);
    }
}
