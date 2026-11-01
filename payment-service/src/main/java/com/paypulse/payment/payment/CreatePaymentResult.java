
package com.paypulse.payment.payment;

import com.paypulse.payment.payment.dto.PaymentResponse;

public record CreatePaymentResult(
        PaymentResponse payment,
        boolean replayed
) {}
