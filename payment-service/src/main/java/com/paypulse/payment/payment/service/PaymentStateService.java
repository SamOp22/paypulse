package com.paypulse.payment.payment.service;

import com.paypulse.payment.common.exception.PaymentNotFoundException;
import com.paypulse.payment.common.exception.PaymentStateConflictException;
import com.paypulse.payment.payment.bank.BankResult;

import com.paypulse.payment.payment.entity.Payment;
import com.paypulse.payment.payment.entity.PaymentStatus;
import com.paypulse.payment.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentStateService {

    private final PaymentRepository paymentRepository;
    private final LedgerService ledgerService;

    public PaymentStateService(
            PaymentRepository paymentRepository,
            LedgerService ledgerService
    ) {
        this.paymentRepository = paymentRepository;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public void startProcessing(
            UUID paymentId,
            UUID merchantId
    ) {

        int updated = paymentRepository.claimForProcessing(
                paymentId,
                merchantId
        );

        if (updated != 1) {
            throw new PaymentStateConflictException(
                    "Payment is not available for processing"
            );
        }
    }

    @Transactional
    public void complete(
            UUID paymentId,
            BankResult result
    ) {

        Payment payment = paymentRepository
                .findLockedById(paymentId)
                .orElseThrow(PaymentNotFoundException::new);

        PaymentStatus target = switch (result) {
            case APPROVED -> PaymentStatus.SUCCESS;
            case DECLINED -> PaymentStatus.FAILED;
        };

        if (payment.getStatus() == target) {
            return;
        }

        if (payment.getStatus() != PaymentStatus.PROCESSING
                && payment.getStatus()
                != PaymentStatus.REQUIRES_RECONCILIATION) {

            throw new PaymentStateConflictException(
                    "Cannot finalize payment from "
                            + payment.getStatus()
            );
        }

        payment.transitionTo(target);

        if (target == PaymentStatus.SUCCESS) {
            ledgerService.recordSuccessfulCharge(payment);
        }
    }

    @Transactional
    public void markUncertain(UUID paymentId) {

        Payment payment = paymentRepository
                .findLockedById(paymentId)
                .orElseThrow(PaymentNotFoundException::new);

        if (payment.getStatus() == PaymentStatus.PROCESSING) {

            payment.transitionTo(
                    PaymentStatus.REQUIRES_RECONCILIATION
            );
        }
    }
}