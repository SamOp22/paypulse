
package com.paypulse.payment.auth.dto;

import com.paypulse.payment.merchant.MerchantResponse;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        MerchantResponse merchant
) {}
