package com.smartpay.merchant.service;

import com.smartpay.merchant.domain.Merchant;
import com.smartpay.merchant.domain.RefreshToken;
import com.smartpay.merchant.domain.exception.InvalidRefreshTokenException;
import com.smartpay.merchant.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void verify_whenTokenIsRevoked_throwsInvalidRefreshTokenException() {
        RefreshToken token = RefreshToken.builder()
                .merchant(new Merchant())
                .tokenHash("hashedtoken")
                .revoked(true)
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .build();

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThrows(InvalidRefreshTokenException.class,
                () -> refreshTokenService.verify("rawtoken"));
    }

    @Test
    void verify_whenTokenIsExpired_throwsInvalidRefreshTokenException() {
        RefreshToken token = RefreshToken.builder()
                .merchant(new Merchant())
                .tokenHash("hashedtoken")
                .revoked(false)
                .expiresAt(OffsetDateTime.now().minusDays(1))
                .build();

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThrows(InvalidRefreshTokenException.class,
                () -> refreshTokenService.verify("rawtoken"));
    }

    @Test
    void verify_whenTokenNotFound_throwsInvalidRefreshTokenException() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class,
                () -> refreshTokenService.verify("rawtoken"));
    }
}