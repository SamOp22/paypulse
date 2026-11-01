
package com.paypulse.payment.merchant.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchants")
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "email", nullable = false,
            unique = true, length = 255)
    private String email;

    @Column(name = "password_hash",
            nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MerchantRole role;

    @Column(name = "created_at",
            nullable = false, updatable = false)
    private Instant createdAt;

    protected Merchant() {
    }

    public Merchant(
            String name,
            String email,
            String passwordHash,
            MerchantRole role
    ) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public MerchantRole getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
