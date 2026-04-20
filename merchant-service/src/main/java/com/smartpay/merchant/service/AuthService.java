package com.smartpay.merchant.service;

import com.smartpay.merchant.domain.Merchant;
import com.smartpay.merchant.domain.RefreshToken;
import com.smartpay.merchant.dto.LoginRequest;
import com.smartpay.merchant.dto.LoginResponse;
import com.smartpay.merchant.dto.RefreshTokenRequest;
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
    private final RefreshTokenService refreshTokenService;

    public LoginResponse login(LoginRequest request) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(request.email(), request.password());
        var authentication = authenticationManager.authenticate(authToken);
        Merchant merchant = (Merchant) authentication.getPrincipal();

        return new LoginResponse(jwtService.generateToken(merchant), refreshTokenService.generate(merchant));
    }

    public LoginResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.verify(request.refreshToken());
        return new LoginResponse(jwtService.generateToken(refreshToken.getMerchant()), refreshTokenService.rotate(refreshToken));
    }
}
