
package com.paypulse.payment.payment;

import com.paypulse.payment.auth.service.JwtService;
import com.paypulse.payment.merchant.entity.Merchant;
import com.paypulse.payment.merchant.entity.MerchantRole;
import com.paypulse.payment.merchant.repository.MerchantRepository;

import com.paypulse.payment.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JsonMapper jsonMapper;

    private Merchant createMerchant() {

        Merchant merchant = new Merchant(
                "Test Merchant",
                "merchant-" + UUID.randomUUID() + "@example.com",
                "test-only-placeholder-hash",
                MerchantRole.MERCHANT
        );

        return merchantRepository.saveAndFlush(merchant);
    }

    private String authorization(Merchant merchant) {
        return "Bearer " + jwtService.generateAccessToken(merchant);
    }

    private String paymentJson(String amount) {
        return """
                {
                  "amount": %s,
                  "currency": "INR",
                  "description": "Test payment"
                }
                """.formatted(amount);
    }

    private MvcResult createPayment(
            Merchant merchant,
            String key,
            String amount
    ) throws Exception {

        return mockMvc.perform(
                        post("/api/payments")
                                .header("Authorization", authorization(merchant))
                                .header("Idempotency-Key", key)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(paymentJson(amount))
                )
                .andReturn();
    }

    @Test
    void shouldCreatePaymentForAuthenticatedMerchant()
            throws Exception {

        Merchant merchant = createMerchant();

        mockMvc.perform(
                        post("/api/payments")
                                .header("Authorization", authorization(merchant))
                                .header("Idempotency-Key", "order-1001")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(paymentJson("1000.00"))
                )
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Idempotency-Replayed", "false"
                ))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.merchantId").value(
                        merchant.getId().toString()
                ));

        assertEquals(
                1,
                paymentRepository.countByMerchantId(merchant.getId())
        );
    }

    @Test
    void shouldReturnSamePaymentForRepeatedKey()
            throws Exception {

        Merchant merchant = createMerchant();

        MvcResult first = createPayment(
                merchant, "order-repeat", "1000.00"
        );

        MvcResult second = createPayment(
                merchant, "order-repeat", "1000.00"
        );

        assertEquals(201, first.getResponse().getStatus());
        assertEquals(201, second.getResponse().getStatus());

        String firstId = jsonMapper
                .readTree(first.getResponse().getContentAsString())
                .get("id").asText();

        String secondId = jsonMapper
                .readTree(second.getResponse().getContentAsString())
                .get("id").asText();

        assertEquals(firstId, secondId);

        assertEquals(
                "true",
                second.getResponse().getHeader("Idempotency-Replayed")
        );

        assertEquals(
                1,
                paymentRepository.countByMerchantId(merchant.getId())
        );
    }

    @Test
    void shouldRejectDifferentPayloadWithSameKey()
            throws Exception {

        Merchant merchant = createMerchant();

        MvcResult first = createPayment(
                merchant, "order-conflict", "1000.00"
        );

        MvcResult second = createPayment(
                merchant, "order-conflict", "2000.00"
        );

        assertEquals(201, first.getResponse().getStatus());
        assertEquals(409, second.getResponse().getStatus());

        assertEquals(
                1,
                paymentRepository.countByMerchantId(merchant.getId())
        );
    }

    @Test
    void shouldPreventCrossMerchantAccess()
            throws Exception {

        Merchant merchantA = createMerchant();
        Merchant merchantB = createMerchant();

        MvcResult created = createPayment(
                merchantA, "merchant-a-order", "500.00"
        );

        assertEquals(201, created.getResponse().getStatus());

        String paymentId = jsonMapper
                .readTree(created.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(
                        get("/api/payments/{id}", paymentId)
                                .header(
                                        "Authorization",
                                        authorization(merchantB)
                                )
                )
                .andExpect(status().isNotFound());

        mockMvc.perform(
                        get("/api/payments")
                                .header(
                                        "Authorization",
                                        authorization(merchantB)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldRejectInvalidAmount() throws Exception {

        Merchant merchant = createMerchant();

        mockMvc.perform(
                        post("/api/payments")
                                .header("Authorization", authorization(merchant))
                                .header("Idempotency-Key", "invalid-order")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(paymentJson("-100.00"))
                )
                .andExpect(status().isBadRequest());

        assertEquals(
                0,
                paymentRepository.countByMerchantId(merchant.getId())
        );
    }

    @Test
    void shouldRequireAuthentication() throws Exception {

        mockMvc.perform(
                        post("/api/payments")
                                .header("Idempotency-Key", "unauthorized-order")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(paymentJson("1000.00"))
                )
                .andExpect(status().isUnauthorized());
    }
}
