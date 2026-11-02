
package com.paypulse.payment.payment.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "merchant_id", nullable = false, updatable = false)
    private UUID merchantId;

    @Column(
            name = "amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "description", length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() {
    }

    public Payment(
            UUID merchantId,
            BigDecimal amount,
            String currency,
            String description
    ) {
        this.merchantId = merchantId;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.status = PaymentStatus.CREATED;
    }

    @PrePersist
    protected void onCreate() {

        Instant now = Instant.now();

        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void transitionTo(PaymentStatus next) {

        boolean allowed = switch (this.status) {

            case CREATED ->
                    next == PaymentStatus.PROCESSING;

            case PROCESSING ->
                    next == PaymentStatus.SUCCESS
                            || next == PaymentStatus.FAILED
                            || next == PaymentStatus.REQUIRES_RECONCILIATION;

            case REQUIRES_RECONCILIATION ->
                    next == PaymentStatus.SUCCESS
                            || next == PaymentStatus.FAILED;

            case SUCCESS, FAILED -> false;
        };

        if (!allowed) {
            throw new IllegalStateException(
                    "Invalid payment transition: "
                            + this.status + " -> " + next
            );
        }

        this.status = next;
    }
}
