
package com.paypulse.payment.payment;

import com.paypulse.payment.merchant.entity.Merchant;
import com.paypulse.payment.merchant.entity.MerchantRole;
import com.paypulse.payment.merchant.repository.MerchantRepository;
import com.paypulse.payment.payment.dto.CreatePaymentRequest;

import com.paypulse.payment.payment.repository.PaymentRepository;
import com.paypulse.payment.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PaymentConcurrencyIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void concurrentRequestsShouldCreateOnlyOnePayment()
            throws Exception {

        Merchant merchant = merchantRepository.saveAndFlush(
                new Merchant(
                        "Concurrency Test",
                        "concurrent-" + UUID.randomUUID() + "@example.com",
                        "test-only-placeholder-hash",
                        MerchantRole.MERCHANT
                )
        );

        UUID merchantId = merchant.getId();

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch startGate = new CountDownLatch(1);

        CreatePaymentRequest request = new CreatePaymentRequest(
                new BigDecimal("1000.00"),
                "INR",
                "Concurrent order"
        );

        Callable<CreatePaymentResult> task = () -> {

            startGate.await();

            return paymentService.createPayment(
                    merchantId,
                    request,
                    "same-concurrent-key"
            );
        };

        try {

            Future<CreatePaymentResult> first =
                    executor.submit(task);

            Future<CreatePaymentResult> second =
                    executor.submit(task);

            startGate.countDown();

            CreatePaymentResult result1 =
                    first.get(30, TimeUnit.SECONDS);

            CreatePaymentResult result2 =
                    second.get(30, TimeUnit.SECONDS);

            assertEquals(
                    result1.payment().id(),
                    result2.payment().id()
            );

            assertEquals(
                    1,
                    paymentRepository.countByMerchantId(merchantId)
            );

            assertNotEquals(
                    result1.replayed(),
                    result2.replayed()
            );

        } finally {

            executor.shutdownNow();

            // Remove only records belonging to this test merchant.
            jdbcTemplate.update(
                    "DELETE FROM idempotency_records WHERE merchant_id = ?",
                    merchantId
            );

            jdbcTemplate.update(
                    "DELETE FROM payments WHERE merchant_id = ?",
                    merchantId
            );

            merchantRepository.deleteById(merchantId);
        }
    }
}
