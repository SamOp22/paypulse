
package com.paypulse.payment.merchant;

import com.paypulse.payment.merchant.entity.Merchant;
import com.paypulse.payment.merchant.entity.MerchantRole;

import java.time.Instant;
import java.util.UUID;

public record MerchantResponse(
        UUID id,
        String name,
        String email,
        MerchantRole role,
        Instant createdAt
) {
    public static MerchantResponse from(Merchant merchant) {
        return new MerchantResponse(
                merchant.getId(),
                merchant.getName(),
                merchant.getEmail(),
                merchant.getRole(),
                merchant.getCreatedAt()
        );
    }
}
