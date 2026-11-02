package com.paypulse.bank.service;

import com.paypulse.bank.dto.BankDtos.ChargeRequest;
import com.paypulse.bank.dto.BankDtos.ChargeResponse;

import com.paypulse.bank.entity.BankScenario;
import com.paypulse.bank.entity.BankStatus;
import com.paypulse.bank.entity.BankTransaction;
import com.paypulse.bank.repository.BankTransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class BankChargeService {

    private final BankTransactionRepository repository;

    public BankChargeService(BankTransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ChargeResponse charge(ChargeRequest request) {

        if (request.scenario() == BankScenario.BANK_UNAVAILABLE) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Simulated bank outage"
            );
        }

        BankStatus status =
                request.scenario() == BankScenario.DECLINED
                        ? BankStatus.DECLINED
                        : BankStatus.APPROVED;

        repository.insertIfAbsent(
                request.paymentReference(),
                request.amount(),
                request.currency(),
                request.scenario().name(),
                status.name()
        );

        BankTransaction transaction = repository
                .findById(request.paymentReference())
                .orElseThrow();

        boolean sameRequest =
                transaction.getAmount().compareTo(request.amount()) == 0
                        && transaction.getCurrency().equals(request.currency())
                        && transaction.getScenario() == request.scenario();

        if (!sameRequest) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment reference already used with different details"
            );
        }

        return new ChargeResponse(
                transaction.getPaymentReference(),
                transaction.getStatus()
        );
    }

    @Transactional(readOnly = true)
    public ChargeResponse findTransaction(UUID reference) {

        BankTransaction transaction = repository.findById(reference)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bank transaction not found"
                ));

        return new ChargeResponse(
                transaction.getPaymentReference(),
                transaction.getStatus()
        );
    }
}