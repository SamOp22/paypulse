
package com.paypulse.payment.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
