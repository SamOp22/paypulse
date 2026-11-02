package com.paypulse.payment.payment.bank;

import com.paypulse.payment.payment.entity.Payment;
import com.paypulse.payment.payment.bank.BankDtos.ChargeRequest;
import com.paypulse.payment.payment.bank.BankDtos.ChargeResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
public class BankClient {

    private final RestClient restClient;

    public BankClient(
            @Value("${app.bank.base-url}") String baseUrl,
            @Value("${app.bank.internal-api-key}") String apiKey,
            @Value("${app.bank.connect-timeout-ms}") int connectTimeout,
            @Value("${app.bank.read-timeout-ms}") int readTimeout
    ) {

        if (apiKey == null || apiKey.isBlank()
                || connectTimeout <= 0 || readTimeout <= 0) {
            throw new IllegalStateException(
                    "Invalid bank client configuration"
            );
        }

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        factory.setReadTimeout(Duration.ofMillis(readTimeout));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("X-Internal-Api-Key", apiKey)
                .build();
    }

    public BankResult charge(
            Payment payment,
            BankScenario scenario
    ) {

        try {
            ChargeResponse response = restClient.post()
                    .uri("/internal/charges")
                    .body(new ChargeRequest(
                            payment.getId(),
                            payment.getAmount(),
                            payment.getCurrency(),
                            scenario
                    ))
                    .retrieve()
                    .body(ChargeResponse.class);

            validateResponse(payment.getId(), response);

            return response.result();

        } catch (RestClientException ex) {
            throw new BankCommunicationException(
                    "Bank charge outcome is uncertain", ex
            );
        }
    }

    public Optional<ChargeResponse> lookupWithRetry(UUID reference) {

        for (int attempt = 1; attempt <= 2; attempt++) {

            try {
                return lookup(reference);

            } catch (BankCommunicationException ex) {

                if (attempt == 2) {
                    throw ex;
                }

                try {
                    Thread.sleep(150);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();

                    throw new BankCommunicationException(
                            "Bank lookup interrupted", interrupted
                    );
                }
            }
        }

        throw new IllegalStateException("Unexpected retry state");
    }

    private Optional<ChargeResponse> lookup(UUID reference) {

        try {
            ChargeResponse response = restClient.get()
                    .uri("/internal/transactions/{reference}", reference)
                    .retrieve()
                    .body(ChargeResponse.class);

            validateResponse(reference, response);

            return Optional.of(response);

        } catch (HttpClientErrorException.NotFound ex) {

            return Optional.empty();

        } catch (RestClientException ex) {

            throw new BankCommunicationException(
                    "Bank lookup failed", ex
            );
        }
    }

    private void validateResponse(
            UUID reference,
            ChargeResponse response
    ) {

        if (response == null
                || response.paymentReference() == null
                || !reference.equals(response.paymentReference())
                || response.result() == null) {

            throw new BankCommunicationException(
                    "Invalid bank response"
            );
        }
    }

    public static class BankCommunicationException
            extends RuntimeException {

        public BankCommunicationException(String message) {
            super(message);
        }

        public BankCommunicationException(
                String message,
                Throwable cause
        ) {
            super(message, cause);
        }
    }
}