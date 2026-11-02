
package com.paypulse.payment.payment.dto;

import com.paypulse.payment.payment.entity.Payment;
import com.paypulse.payment.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID merchantId,
        BigDecimal amount,
        String currency,
        String description,
        PaymentStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static PaymentResponse from(Payment payment) {

        return new PaymentResponse(
                payment.getId(),
                payment.getMerchantId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getDescription(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
