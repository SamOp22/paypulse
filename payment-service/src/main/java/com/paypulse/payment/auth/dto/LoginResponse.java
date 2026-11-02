
package com.paypulse.payment.auth.dto;

import com.paypulse.payment.merchant.dto.MerchantResponse;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        MerchantResponse merchant
) {}
