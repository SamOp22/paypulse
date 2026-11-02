package com.paypulse.bank;

import com.paypulse.bank.dto.BankDtos.ChargeRequest;
import com.paypulse.bank.dto.BankDtos.ChargeResponse;

import com.paypulse.bank.entity.BankScenario;
import com.paypulse.bank.entity.BankStatus;
import com.paypulse.bank.repository.BankTransactionRepository;
import com.paypulse.bank.service.BankChargeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class BankChargeServiceIntegrationTest {

    @Autowired
    private BankChargeService bankChargeService;

    @Autowired
    private BankTransactionRepository repository;

    @Test
    void shouldApproveAndPersistCharge() {

        UUID reference = UUID.randomUUID();

        ChargeRequest request = new ChargeRequest(
                reference,
                new BigDecimal("1000.00"),
                "INR",
                BankScenario.SUCCESS
        );

        ChargeResponse response = bankChargeService.charge(request);

        assertEquals(BankStatus.APPROVED, response.result());
        assertEquals(reference, response.paymentReference());
        assertTrue(repository.existsById(reference));
    }

    @Test
    void shouldReturnSameResultForDuplicateCharge() {

        UUID reference = UUID.randomUUID();

        ChargeRequest request = new ChargeRequest(
                reference,
                new BigDecimal("750.00"),
                "INR",
                BankScenario.SUCCESS
        );

        ChargeResponse first = bankChargeService.charge(request);
        ChargeResponse second = bankChargeService.charge(request);

        assertEquals(first.paymentReference(), second.paymentReference());
        assertEquals(first.result(), second.result());

        assertEquals(
                BankStatus.APPROVED,
                repository.findById(reference).orElseThrow().getStatus()
        );
    }

    @Test
    void shouldRejectDifferentRequestUsingSameReference() {

        UUID reference = UUID.randomUUID();

        bankChargeService.charge(new ChargeRequest(
                reference,
                new BigDecimal("1000.00"),
                "INR",
                BankScenario.SUCCESS
        ));

        assertThrows(
                ResponseStatusException.class,
                () -> bankChargeService.charge(new ChargeRequest(
                        reference,
                        new BigDecimal("2000.00"),
                        "INR",
                        BankScenario.SUCCESS
                ))
        );
    }

    @Test
    void shouldRecordDeclinedCharge() {

        UUID reference = UUID.randomUUID();

        ChargeResponse response = bankChargeService.charge(
                new ChargeRequest(
                        reference,
                        new BigDecimal("500.00"),
                        "INR",
                        BankScenario.DECLINED
                )
        );

        assertEquals(BankStatus.DECLINED, response.result());
    }

    @Test
    void timeoutScenarioShouldPersistApproval() {

        UUID reference = UUID.randomUUID();

        ChargeResponse response = bankChargeService.charge(
                new ChargeRequest(
                        reference,
                        new BigDecimal("900.00"),
                        "INR",
                        BankScenario.TIMEOUT_AFTER_COMMIT
                )
        );

        assertEquals(BankStatus.APPROVED, response.result());

        assertEquals(
                BankStatus.APPROVED,
                repository.findById(reference).orElseThrow().getStatus()
        );
    }
}