package com.smartpay.merchant.controller;

import com.smartpay.merchant.dto.*;
import com.smartpay.merchant.service.AuthService;
import com.smartpay.merchant.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/auth")
public class AuthController {

    private final MerchantService merchantService;
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterMerchantResponse> register(@Valid @RequestBody RegisterMerchantRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(merchantService.registerMerchant(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity
                .ok()
                .body(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity
                .ok()
                .body(authService.refresh(request));
    }
}
