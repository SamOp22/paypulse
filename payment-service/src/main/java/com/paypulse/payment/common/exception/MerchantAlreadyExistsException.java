
package com.paypulse.payment.common.exception;

public class MerchantAlreadyExistsException
        extends RuntimeException {

    public MerchantAlreadyExistsException() {
        super("A merchant with this email already exists");
    }
}
