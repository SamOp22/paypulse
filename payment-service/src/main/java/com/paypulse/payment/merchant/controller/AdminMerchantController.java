
package com.paypulse.payment.merchant.controller;

import com.paypulse.payment.merchant.service.MerchantService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/merchants")
public class AdminMerchantController {

    private final MerchantService merchantService;

    public AdminMerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Long> countMerchants() {

        return Map.of(
                "count", merchantService.countMerchants()
        );
    }
}
