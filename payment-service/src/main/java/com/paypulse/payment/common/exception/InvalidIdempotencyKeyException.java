
package com.paypulse.payment.common.exception;

public class InvalidIdempotencyKeyException extends RuntimeException {

    public InvalidIdempotencyKeyException() {
        super(
                "Idempotency-Key must contain 1-128 letters, digits, dots, underscores or hyphens"
        );
    }
}
