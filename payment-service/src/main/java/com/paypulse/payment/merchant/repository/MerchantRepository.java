
package com.paypulse.payment.merchant.repository;

import com.paypulse.payment.merchant.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository
        extends JpaRepository<Merchant, UUID> {

    Optional<Merchant> findByEmail(String email);

    boolean existsByEmail(String email);
}
