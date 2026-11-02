package com.paypulse.bank.controller;

import com.paypulse.bank.dto.BankDtos.ChargeRequest;
import com.paypulse.bank.dto.BankDtos.ChargeResponse;
import com.paypulse.bank.entity.BankScenario;
import com.paypulse.bank.service.BankChargeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal")
public class BankController {

    private final BankChargeService bankChargeService;

    public BankController(BankChargeService bankChargeService) {
        this.bankChargeService = bankChargeService;
    }

    @PostMapping("/charges")
    public ChargeResponse charge(
            @Valid @RequestBody ChargeRequest request) {

        ChargeResponse response = bankChargeService.charge(request);

        if (request.scenario() ==
                BankScenario.TIMEOUT_AFTER_COMMIT) {

            try {
                Thread.sleep(2500);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        return response;
    }

    @GetMapping("/transactions/{reference}")
    public ChargeResponse getTransaction(
            @PathVariable UUID reference) {

        return bankChargeService.findTransaction(reference);
    }
}