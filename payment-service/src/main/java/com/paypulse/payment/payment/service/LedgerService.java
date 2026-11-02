package com.paypulse.payment.payment.service;

import com.paypulse.payment.payment.entity.Payment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class LedgerService {

    private final JdbcTemplate jdbcTemplate;

    public LedgerService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void recordSuccessfulCharge(Payment payment) {

        BigDecimal amount = payment.getAmount();

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Ledger amount must be positive"
            );
        }

        UUID journalId = UUID.randomUUID();

        int inserted = jdbcTemplate.update("""
            INSERT INTO ledger_journals (
                id,
                payment_id,
                reference_type,
                reference_id,
                event_type
            )
            VALUES (?, ?, 'CHARGE', ?, 'PAYMENT_CAPTURED')
            ON CONFLICT (reference_type, reference_id)
            DO NOTHING
            """,
                journalId,
                payment.getId(),
                payment.getId()
        );

        if (inserted == 0) {
            return;
        }

        insertEntry(
                journalId,
                "BANK_CLEARING",
                "DEBIT",
                amount
        );

        insertEntry(
                journalId,
                "MERCHANT_PAYABLE",
                "CREDIT",
                amount
        );
    }

    private void insertEntry(
            UUID journalId,
            String account,
            String direction,
            BigDecimal amount
    ) {

        jdbcTemplate.update("""
            INSERT INTO ledger_entries (
                id,
                journal_id,
                account_code,
                direction,
                amount
            )
            VALUES (?, ?, ?, ?, ?)
            """,
                UUID.randomUUID(),
                journalId,
                account,
                direction,
                amount
        );
    }
}