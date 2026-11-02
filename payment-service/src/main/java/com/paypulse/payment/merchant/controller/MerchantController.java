
package com.paypulse.payment.merchant.controller;

import com.paypulse.payment.merchant.dto.MerchantResponse;
import com.paypulse.payment.merchant.service.MerchantService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @GetMapping("/me")
    public MerchantResponse getCurrentMerchant(
            @AuthenticationPrincipal Jwt jwt) {

        UUID merchantId = UUID.fromString(jwt.getSubject());

        return merchantService.getMerchant(merchantId);
    }
}
