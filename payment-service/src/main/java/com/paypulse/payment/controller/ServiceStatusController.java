
package com.paypulse.payment.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/status")
public class ServiceStatusController {

    @GetMapping
    public Map<String, String> getStatus() {

        return Map.of(
                "service", "payment-service",
                "status", "RUNNING"
        );
    }
}
