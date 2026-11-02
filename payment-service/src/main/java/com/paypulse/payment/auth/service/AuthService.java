
package com.paypulse.payment.auth.service;

import com.paypulse.payment.auth.dto.LoginRequest;
import com.paypulse.payment.auth.dto.LoginResponse;
import com.paypulse.payment.merchant.dto.MerchantResponse;
import com.paypulse.payment.merchant.entity.Merchant;
import com.paypulse.payment.merchant.repository.MerchantRepository;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final MerchantRepository merchantRepository;
    private final JwtService jwtService;

    public AuthService(
            AuthenticationManager authenticationManager,
            MerchantRepository merchantRepository,
            JwtService jwtService) {

        this.authenticationManager = authenticationManager;
        this.merchantRepository = merchantRepository;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken
                        .unauthenticated(email, request.password())
        );

        Merchant merchant = merchantRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid email or password"
                        )
                );

        String token = jwtService.generateAccessToken(merchant);

        return new LoginResponse(
                token,
                "Bearer",
                jwtService.getAccessTokenLifetimeSeconds(),
                MerchantResponse.from(merchant)
        );
    }
}
