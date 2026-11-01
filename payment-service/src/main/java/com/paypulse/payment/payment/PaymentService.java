
package com.paypulse.payment.payment;

import com.paypulse.payment.common.exception.IdempotencyConflictException;
import com.paypulse.payment.common.exception.InvalidIdempotencyKeyException;
import com.paypulse.payment.common.exception.MerchantNotFoundException;
import com.paypulse.payment.common.exception.PaymentNotFoundException;
import com.paypulse.payment.merchant.repository.MerchantRepository;
import com.paypulse.payment.payment.dto.CreatePaymentRequest;
import com.paypulse.payment.payment.dto.PaymentResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final MerchantRepository merchantRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            IdempotencyRepository idempotencyRepository,
            MerchantRepository merchantRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.merchantRepository = merchantRepository;
    }

    @Transactional
    public CreatePaymentResult createPayment(
            UUID merchantId,
            CreatePaymentRequest request,
            String idempotencyKey
    ) {

        validateIdempotencyKey(idempotencyKey);

        if (!merchantRepository.existsById(merchantId)) {
            throw new MerchantNotFoundException();
        }

        BigDecimal amount = request.amount()
                .setScale(2, RoundingMode.UNNECESSARY);

        String currency = request.currency();

        String description = normalizeDescription(
                request.description()
        );

        String requestHash = calculateRequestHash(
                amount, currency, description
        );

        // Step 1: Atomically reserve the idempotency key.
        int inserted = idempotencyRepository.reserveKey(
                UUID.randomUUID(),
                merchantId,
                idempotencyKey,
                requestHash
        );

        // Step 2: If the key already exists, reuse its payment.
        if (inserted == 0) {

            IdempotencyRecord existing = idempotencyRepository
                    .findByMerchantIdAndIdempotencyKey(
                            merchantId, idempotencyKey
                    )
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "Idempotency record not found"
                            )
                    );

            if (!existing.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException();
            }

            if (existing.getPaymentId() == null) {
                throw new IllegalStateException(
                        "Idempotency record has no payment"
                );
            }

            Payment payment = paymentRepository
                    .findByIdAndMerchantId(
                            existing.getPaymentId(), merchantId
                    )
                    .orElseThrow(PaymentNotFoundException::new);

            return new CreatePaymentResult(
                    PaymentResponse.from(payment),
                    true
            );
        }

        // Step 3: Create a new payment.
        Payment payment = new Payment(
                merchantId,
                amount,
                currency,
                description
        );

        Payment saved = paymentRepository.saveAndFlush(payment);

        // Step 4: Associate the new payment with the reserved key.
        int linked = idempotencyRepository.attachPayment(
                merchantId,
                idempotencyKey,
                saved.getId()
        );

        if (linked != 1) {
            throw new IllegalStateException(
                    "Failed to link idempotency record"
            );
        }

        return new CreatePaymentResult(
                PaymentResponse.from(saved),
                false
        );
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(
            UUID paymentId,
            UUID merchantId
    ) {

        Payment payment = paymentRepository
                .findByIdAndMerchantId(paymentId, merchantId)
                .orElseThrow(PaymentNotFoundException::new);

        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> listPayments(
            UUID merchantId,
            int page,
            int size
    ) {

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        return paymentRepository
                .findByMerchantId(merchantId, pageable)
                .map(PaymentResponse::from);
    }

    private void validateIdempotencyKey(String key) {

        if (key == null ||
                !key.matches("[A-Za-z0-9._-]{1,128}")) {

            throw new InvalidIdempotencyKeyException();
        }
    }

    private String normalizeDescription(String description) {

        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }

    private String calculateRequestHash(
            BigDecimal amount,
            String currency,
            String description
    ) {

        String normalizedDescription =
                description == null ? "" : description;

        String canonicalRequest =
                amount.toPlainString() + "|" +
                        currency + "|" +
                        normalizedDescription.length() + ":" +
                        normalizedDescription;

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    canonicalRequest.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "SHA-256 not available", ex
            );
        }
    }
}
