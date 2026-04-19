package com.smartpay.merchant.service;

import com.smartpay.merchant.domain.Merchant;
import com.smartpay.merchant.dto.LoginRequest;
import com.smartpay.merchant.dto.LoginResponse;
import com.smartpay.merchant.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(request.email(), request.password());
        var authentication = authenticationManager.authenticate(authToken);
        Merchant merchant = (Merchant) authentication.getPrincipal();

        return new LoginResponse(jwtService.generateToken(merchant));
    }
}
