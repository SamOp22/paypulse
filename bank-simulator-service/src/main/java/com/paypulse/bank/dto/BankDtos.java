package com.paypulse.bank.dto;

import com.paypulse.bank.entity.BankScenario;
import com.paypulse.bank.entity.BankStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public final class BankDtos {

    private BankDtos() {
    }

    public record ChargeRequest(
            @NotNull UUID paymentReference,

            @NotNull
            @DecimalMin("0.01")
            @Digits(integer = 17, fraction = 2)
            BigDecimal amount,

            @NotBlank String currency,

            @NotNull BankScenario scenario
    ) {}

    public record ChargeResponse(
            UUID paymentReference,
            BankStatus result
    ) {}
}