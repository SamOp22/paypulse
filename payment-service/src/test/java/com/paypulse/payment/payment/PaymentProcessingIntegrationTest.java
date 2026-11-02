package com.paypulse.payment.payment;

import com.paypulse.payment.auth.service.JwtService;
import com.paypulse.payment.merchant.entity.Merchant;
import com.paypulse.payment.merchant.entity.MerchantRole;
import com.paypulse.payment.merchant.repository.MerchantRepository;
import com.paypulse.payment.payment.bank.*;

import com.paypulse.payment.payment.entity.Payment;
import com.paypulse.payment.payment.repository.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentProcessingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private BankClient bankClient;

    private Merchant merchant;
    private Payment payment;

    @BeforeEach
    void setup() {

        merchant = merchantRepository.saveAndFlush(
                new Merchant(
                        "Processing Test",
                        "processing-" + UUID.randomUUID() + "@example.com",
                        "test-only-placeholder-hash",
                        MerchantRole.MERCHANT
                )
        );

        payment = paymentRepository.saveAndFlush(
                new Payment(
                        merchant.getId(),
                        new BigDecimal("1000.00"),
                        "INR",
                        "Processing integration test"
                )
        );
    }

    @AfterEach
    void cleanup() {

        UUID paymentId = payment.getId();

        jdbcTemplate.update("""
            DELETE FROM ledger_entries
            WHERE journal_id IN (
                SELECT id FROM ledger_journals
                WHERE payment_id = ?
            )
            """, paymentId);

        jdbcTemplate.update(
                "DELETE FROM ledger_journals WHERE payment_id = ?",
                paymentId
        );

        jdbcTemplate.update(
                "DELETE FROM idempotency_records WHERE payment_id = ?",
                paymentId
        );

        jdbcTemplate.update(
                "DELETE FROM payments WHERE id = ?",
                paymentId
        );

        merchantRepository.deleteById(merchant.getId());
    }

    private String authorization() {
        return "Bearer " + jwtService.generateAccessToken(merchant);
    }

    private int ledgerEntryCount() {
        return jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM ledger_entries e
            JOIN ledger_journals j ON e.journal_id = j.id
            WHERE j.payment_id = ?
            """, Integer.class, payment.getId());
    }

    @Test
    void successfulPaymentShouldCreateBalancedLedger()
            throws Exception {

        when(bankClient.charge(
                any(Payment.class),
                eq(BankScenario.SUCCESS)
        )).thenReturn(BankResult.APPROVED);

        mockMvc.perform(
                        post("/api/payments/{id}/process", payment.getId())
                                .header("Authorization", authorization())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {"scenario":"SUCCESS"}
                            """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        assertEquals(2, ledgerEntryCount());

        BigDecimal debits = jdbcTemplate.queryForObject("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM ledger_entries e
            JOIN ledger_journals j ON e.journal_id = j.id
            WHERE j.payment_id = ? AND e.direction = 'DEBIT'
            """, BigDecimal.class, payment.getId());

        BigDecimal credits = jdbcTemplate.queryForObject("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM ledger_entries e
            JOIN ledger_journals j ON e.journal_id = j.id
            WHERE j.payment_id = ? AND e.direction = 'CREDIT'
            """, BigDecimal.class, payment.getId());

        assertEquals(0, debits.compareTo(credits));
        assertEquals(0, debits.compareTo(new BigDecimal("1000.00")));
    }

    @Test
    void declinedPaymentShouldNotCreateLedgerEntries()
            throws Exception {

        when(bankClient.charge(
                any(Payment.class),
                eq(BankScenario.DECLINED)
        )).thenReturn(BankResult.DECLINED);

        mockMvc.perform(
                        post("/api/payments/{id}/process", payment.getId())
                                .header("Authorization", authorization())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {"scenario":"DECLINED"}
                            """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));

        assertEquals(0, ledgerEntryCount());
    }

    @Test
    void timeoutShouldBeReconciledUsingBankResult()
            throws Exception {

        when(bankClient.charge(
                any(Payment.class),
                eq(BankScenario.TIMEOUT_AFTER_COMMIT)
        )).thenThrow(new BankClient.BankCommunicationException(
                "Simulated read timeout"
        ));

        mockMvc.perform(
                        post("/api/payments/{id}/process", payment.getId())
                                .header("Authorization", authorization())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {"scenario":"TIMEOUT_AFTER_COMMIT"}
                            """)
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status")
                        .value("REQUIRES_RECONCILIATION"));

        when(bankClient.lookupWithRetry(payment.getId()))
                .thenReturn(Optional.of(
                        new BankDtos.ChargeResponse(
                                payment.getId(),
                                BankResult.APPROVED
                        )
                ));

        mockMvc.perform(
                        post("/api/payments/{id}/reconcile", payment.getId())
                                .header("Authorization", authorization())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        assertEquals(2, ledgerEntryCount());

        // Reconciliation again must not duplicate the ledger.
        mockMvc.perform(
                        post("/api/payments/{id}/reconcile", payment.getId())
                                .header("Authorization", authorization())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        assertEquals(2, ledgerEntryCount());
    }

    @Test
    void shouldPreventProcessingSamePaymentTwice()
            throws Exception {

        when(bankClient.charge(
                any(Payment.class),
                eq(BankScenario.SUCCESS)
        )).thenReturn(BankResult.APPROVED);

        String body = """
                {"scenario":"SUCCESS"}
                """;

        mockMvc.perform(
                        post("/api/payments/{id}/process", payment.getId())
                                .header("Authorization", authorization())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/payments/{id}/process", payment.getId())
                                .header("Authorization", authorization())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isConflict());

        verify(bankClient, times(1))
                .charge(any(Payment.class), eq(BankScenario.SUCCESS));

        assertEquals(2, ledgerEntryCount());
    }
}