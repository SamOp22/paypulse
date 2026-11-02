package com.paypulse.bank.repository;


import com.paypulse.bank.entity.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

public interface BankTransactionRepository
        extends JpaRepository<BankTransaction, UUID> {

    @Modifying
    @Query(value = """
        INSERT INTO bank_transactions (
            payment_reference,
            amount,
            currency,
            scenario,
            status,
            created_at
        )
        VALUES (
            :reference,
            :amount,
            :currency,
            :scenario,
            :status,
            CURRENT_TIMESTAMP
        )
        ON CONFLICT (payment_reference) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(
            @Param("reference") UUID reference,
            @Param("amount") BigDecimal amount,
            @Param("currency") String currency,
            @Param("scenario") String scenario,
            @Param("status") String status
    );
}