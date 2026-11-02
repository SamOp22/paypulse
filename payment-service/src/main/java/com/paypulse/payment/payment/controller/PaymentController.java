package com.paypulse.payment.payment.controller;

import com.paypulse.payment.payment.CreatePaymentResult;
import com.paypulse.payment.payment.dto.CreatePaymentRequest;
import com.paypulse.payment.payment.dto.PaymentResponse;
import com.paypulse.payment.payment.dto.ProcessPaymentRequest;

import com.paypulse.payment.payment.entity.PaymentStatus;
import com.paypulse.payment.payment.service.PaymentProcessingService;
import com.paypulse.payment.payment.service.PaymentService;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentProcessingService processingService;

    public PaymentController(
            PaymentService paymentService,
            PaymentProcessingService processingService
    ) {
        this.paymentService = paymentService;
        this.processingService = processingService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        UUID merchantId = UUID.fromString(jwt.getSubject());

        CreatePaymentResult result = paymentService.createPayment(
                merchantId, request, idempotencyKey
        );

        URI location = URI.create(
                "/api/payments/" + result.payment().id()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .location(location)
                .header(
                        "Idempotency-Replayed",
                        Boolean.toString(result.replayed())
                )
                .body(result.payment());
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPayment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID paymentId
    ) {
        UUID merchantId = UUID.fromString(jwt.getSubject());

        return paymentService.getPayment(paymentId, merchantId);
    }

    @GetMapping
    public Page<PaymentResponse> listPayments(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (page < 0 || size < 1 || size > 50) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be >= 0 and size must be between 1 and 50"
            );
        }

        UUID merchantId = UUID.fromString(jwt.getSubject());

        return paymentService.listPayments(
                merchantId, page, size
        );
    }

    @PostMapping("/{paymentId}/process")
    public ResponseEntity<PaymentResponse> processPayment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID paymentId,
            @Valid @RequestBody ProcessPaymentRequest request
    ) {
        UUID merchantId = UUID.fromString(jwt.getSubject());

        PaymentResponse response = processingService.process(
                paymentId,
                merchantId,
                request.scenario()
        );

        return processingResponse(response);
    }

    @PostMapping("/{paymentId}/reconcile")
    public ResponseEntity<PaymentResponse> reconcilePayment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID paymentId
    ) {
        UUID merchantId = UUID.fromString(jwt.getSubject());

        PaymentResponse response = processingService.reconcile(
                paymentId, merchantId
        );

        return processingResponse(response);
    }

    private ResponseEntity<PaymentResponse> processingResponse(
            PaymentResponse response
    ) {
        if (response.status()
                == PaymentStatus.REQUIRES_RECONCILIATION) {

            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(response);
        }

        return ResponseEntity.ok(response);
    }
}