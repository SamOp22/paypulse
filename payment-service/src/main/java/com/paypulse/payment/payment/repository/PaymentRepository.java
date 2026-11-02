
package com.paypulse.payment.payment.repository;

import com.paypulse.payment.payment.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository
        extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdAndMerchantId(
            UUID id,
            UUID merchantId
    );

    Page<Payment> findByMerchantId(
            UUID merchantId,
            Pageable pageable
    );

    long countByMerchantId(UUID merchantId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    UPDATE payments
    SET status = 'PROCESSING',
        updated_at = CURRENT_TIMESTAMP
    WHERE id = :paymentId
      AND merchant_id = :merchantId
      AND status = 'CREATED'
    """, nativeQuery = true)
    int claimForProcessing(
            @Param("paymentId") UUID paymentId,
            @Param("merchantId") UUID merchantId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT p
    FROM Payment p
    WHERE p.id = :paymentId
    """)
    Optional<Payment> findLockedById(
            @Param("paymentId") UUID paymentId
    );
}
