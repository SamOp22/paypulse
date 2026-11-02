package com.paypulse.payment.payment.bank;

import java.math.BigDecimal;
import java.util.UUID;

public final class BankDtos {

    private BankDtos() {
    }

    public record ChargeRequest(
            UUID paymentReference,
            BigDecimal amount,
            String currency,
            BankScenario scenario
    ) {}

    public record ChargeResponse(
            UUID paymentReference,
            BankResult result
    ) {}
}