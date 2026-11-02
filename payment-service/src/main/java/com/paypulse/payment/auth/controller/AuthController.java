
package com.paypulse.payment.auth.controller;

import com.paypulse.payment.auth.service.AuthService;
import com.paypulse.payment.auth.dto.LoginRequest;
import com.paypulse.payment.auth.dto.LoginResponse;
import com.paypulse.payment.auth.dto.RegisterRequest;
import com.paypulse.payment.merchant.dto.MerchantResponse;
import com.paypulse.payment.merchant.service.MerchantService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final MerchantService merchantService;
    private final AuthService authService;

    public AuthController(
            MerchantService merchantService,
            AuthService authService) {

        this.merchantService = merchantService;
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<MerchantResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        MerchantResponse response =
                merchantService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request) {

        return authService.login(request);
    }
}
