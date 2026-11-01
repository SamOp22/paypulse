
package com.paypulse.payment.payment.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreatePaymentRequest(

        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount,

        @NotBlank
        @Pattern(
                regexp = "INR",
                message = "Only INR is supported currently"
        )
        String currency,

        @Size(max = 200)
        String description

) {}
