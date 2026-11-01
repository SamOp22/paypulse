
package com.paypulse.payment.merchant.repository;

import com.paypulse.payment.merchant.entity.Merchant;
import com.paypulse.payment.merchant.entity.MerchantRole;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MerchantRepositoryIntegrationTest {

    @Autowired
    private MerchantRepository merchantRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void shouldSaveAndFindMerchantByEmail() {

        String email =
                "test-" + UUID.randomUUID() + "@paypulse.test";

        Merchant merchant = new Merchant(
                "Demo Merchant",
                email,
                "test-only-placeholder-hash",
                MerchantRole.MERCHANT
        );

        Merchant saved =
                merchantRepository.saveAndFlush(merchant);

        UUID savedId = saved.getId();

        entityManager.clear();

        Merchant found = merchantRepository
                .findByEmail(email)
                .orElseThrow();

        assertEquals(savedId, found.getId());
        assertEquals("Demo Merchant", found.getName());
        assertEquals(email, found.getEmail());
        assertEquals(MerchantRole.MERCHANT, found.getRole());
        assertNotNull(found.getCreatedAt());
    }
}
