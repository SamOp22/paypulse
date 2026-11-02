
package com.paypulse.payment.merchant.service;

import com.paypulse.payment.auth.dto.RegisterRequest;
import com.paypulse.payment.common.exception.MerchantAlreadyExistsException;
import com.paypulse.payment.common.exception.MerchantNotFoundException;
import com.paypulse.payment.merchant.dto.MerchantResponse;
import com.paypulse.payment.merchant.entity.Merchant;
import com.paypulse.payment.merchant.entity.MerchantRole;
import com.paypulse.payment.merchant.repository.MerchantRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

@Service
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;

    public MerchantService(
            MerchantRepository merchantRepository,
            PasswordEncoder passwordEncoder) {

        this.merchantRepository = merchantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public MerchantResponse register(RegisterRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        String name = request.name().trim();

        if (merchantRepository.existsByEmail(email)) {
            throw new MerchantAlreadyExistsException();
        }

        if (request.password()
                .getBytes(StandardCharsets.UTF_8).length > 72) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password exceeds BCrypt's 72-byte limit"
            );
        }

        String hashedPassword =
                passwordEncoder.encode(request.password());

        Merchant merchant = new Merchant(
                name,
                email,
                hashedPassword,
                MerchantRole.MERCHANT
        );

        Merchant savedMerchant =
                merchantRepository.saveAndFlush(merchant);

        return MerchantResponse.from(savedMerchant);
    }

    @Transactional(readOnly = true)
    public MerchantResponse getMerchant(UUID merchantId) {

        Merchant merchant = merchantRepository
                .findById(merchantId)
                .orElseThrow(MerchantNotFoundException::new);

        return MerchantResponse.from(merchant);
    }

    @Transactional(readOnly = true)
    public long countMerchants() {
        return merchantRepository.count();
    }
}
