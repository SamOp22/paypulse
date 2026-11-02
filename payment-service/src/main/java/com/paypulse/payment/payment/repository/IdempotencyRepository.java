
package com.paypulse.payment.payment.repository;

import com.paypulse.payment.payment.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRepository
        extends JpaRepository<IdempotencyRecord, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO idempotency_records (
                id,
                merchant_id,
                idempotency_key,
                request_hash,
                created_at
            )
            VALUES (
                :id,
                :merchantId,
                :idempotencyKey,
                :requestHash,
                CURRENT_TIMESTAMP
            )
            ON CONFLICT (merchant_id, idempotency_key)
            DO NOTHING
            """, nativeQuery = true)
    int reserveKey(
            @Param("id") UUID id,
            @Param("merchantId") UUID merchantId,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("requestHash") String requestHash
    );

    @Modifying
    @Query(value = """
            UPDATE idempotency_records
            SET payment_id = :paymentId
            WHERE merchant_id = :merchantId
              AND idempotency_key = :idempotencyKey
              AND payment_id IS NULL
            """, nativeQuery = true)
    int attachPayment(
            @Param("merchantId") UUID merchantId,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("paymentId") UUID paymentId
    );

    Optional<IdempotencyRecord> findByMerchantIdAndIdempotencyKey(
            UUID merchantId,
            String idempotencyKey
    );
}
