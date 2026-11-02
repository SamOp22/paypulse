package com.paypulse.payment.common.exception;

public class PaymentStateConflictException
        extends RuntimeException {

    public PaymentStateConflictException(String message) {
        super(message);
    }
}