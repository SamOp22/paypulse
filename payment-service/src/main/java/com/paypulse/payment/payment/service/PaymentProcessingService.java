package com.paypulse.payment.payment.service;

import com.paypulse.payment.common.exception.PaymentNotFoundException;
import com.paypulse.payment.common.exception.PaymentStateConflictException;
import com.paypulse.payment.payment.bank.BankClient;
import com.paypulse.payment.payment.bank.BankResult;
import com.paypulse.payment.payment.bank.BankScenario;
import com.paypulse.payment.payment.dto.PaymentResponse;

import com.paypulse.payment.payment.entity.Payment;
import com.paypulse.payment.payment.entity.PaymentStatus;
import com.paypulse.payment.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentProcessingService {

    private static final Logger log =
            LoggerFactory.getLogger(PaymentProcessingService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final PaymentStateService stateService;
    private final BankClient bankClient;

    public PaymentProcessingService(
            PaymentRepository paymentRepository,
            PaymentService paymentService,
            PaymentStateService stateService,
            BankClient bankClient
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.stateService = stateService;
        this.bankClient = bankClient;
    }

    public PaymentResponse process(
            UUID paymentId,
            UUID merchantId,
            BankScenario scenario
    ) {
        Payment payment = findOwnedPayment(paymentId, merchantId);

        if (payment.getStatus() != PaymentStatus.CREATED) {
            throw new PaymentStateConflictException(
                    "Only CREATED payments can be processed"
            );
        }

        stateService.startProcessing(paymentId, merchantId);

        try {
            BankResult result = bankClient.charge(payment, scenario);
            stateService.complete(paymentId, result);

        } catch (BankClient.BankCommunicationException ex) {
            log.warn("Uncertain bank outcome for payment {}: {}",
                    paymentId, ex.getMessage());

            stateService.markUncertain(paymentId);
        }

        return paymentService.getPayment(paymentId, merchantId);
    }

    public PaymentResponse reconcile(
            UUID paymentId,
            UUID merchantId
    ) {
        Payment payment = findOwnedPayment(paymentId, merchantId);

        if (payment.getStatus() == PaymentStatus.CREATED) {
            throw new PaymentStateConflictException(
                    "Payment has not been processed"
            );
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS
                || payment.getStatus() == PaymentStatus.FAILED) {
            return PaymentResponse.from(payment);
        }

        try {
            var bankTransaction = bankClient.lookupWithRetry(paymentId);

            if (bankTransaction.isPresent()) {
                stateService.complete(
                        paymentId,
                        bankTransaction.get().result()
                );
            } else {
                stateService.markUncertain(paymentId);
            }

        } catch (BankClient.BankCommunicationException ex) {
            log.warn("Reconciliation temporarily unavailable for {}",
                    paymentId);

            stateService.markUncertain(paymentId);
        }

        return paymentService.getPayment(paymentId, merchantId);
    }

    private Payment findOwnedPayment(
            UUID paymentId,
            UUID merchantId
    ) {
        return paymentRepository
                .findByIdAndMerchantId(paymentId, merchantId)
                .orElseThrow(PaymentNotFoundException::new);
    }
}