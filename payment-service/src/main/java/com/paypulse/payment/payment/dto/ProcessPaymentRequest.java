package com.paypulse.payment.payment.dto;

import com.paypulse.payment.payment.bank.BankScenario;
import jakarta.validation.constraints.NotNull;

public record ProcessPaymentRequest(
        @NotNull BankScenario scenario
) {}