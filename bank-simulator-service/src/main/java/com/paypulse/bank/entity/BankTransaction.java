package com.paypulse.bank.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bank_transactions")
public class BankTransaction {

    @Id
    @Column(name = "payment_reference")
    private UUID paymentReference;

    @Column(name = "amount", precision = 19, scale = 2,
            nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "scenario", length = 32, nullable = false)
    private BankScenario scenario;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private BankStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected BankTransaction() {
    }

    public UUID getPaymentReference() { return paymentReference; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public BankScenario getScenario() { return scenario; }
    public BankStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}