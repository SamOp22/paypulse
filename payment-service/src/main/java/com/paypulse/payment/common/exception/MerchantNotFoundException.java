
package com.paypulse.payment.common.exception;

public class MerchantNotFoundException
        extends RuntimeException {

    public MerchantNotFoundException() {
        super("Merchant not found");
    }
}
