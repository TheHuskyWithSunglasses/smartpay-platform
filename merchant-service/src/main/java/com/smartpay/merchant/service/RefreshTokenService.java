package com.smartpay.merchant.service;

import com.smartpay.merchant.domain.Merchant;
import com.smartpay.merchant.domain.RefreshToken;
import com.smartpay.merchant.domain.exception.InvalidRefreshTokenException;
import com.smartpay.merchant.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generate(Merchant merchant) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String result = Base64.getEncoder().encodeToString(randomBytes);

        String tokenHash = hashToken(result);

        OffsetDateTime expiresAt = OffsetDateTime.now().plusMonths(1);

        RefreshToken refreshToken = RefreshToken.builder()
                .merchant(merchant)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return result;
    }

    public RefreshToken verify(String rawToken) {
        String hashedToken = hashToken(rawToken);
        Optional<RefreshToken> refreshTokenOptional = refreshTokenRepository.findByTokenHash(hashedToken);

        if(refreshTokenOptional.isEmpty()) throw new InvalidRefreshTokenException();

        RefreshToken refreshToken = refreshTokenOptional.get();

        if(refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new InvalidRefreshTokenException();
        }

        return refreshToken;
    }

    public String rotate(RefreshToken refreshToken) {
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        return generate(refreshToken.getMerchant());
    }

    private String hashToken(String rawToken) {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA3-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        final byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

        HexFormat hexFormat = HexFormat.of();
        return hexFormat.formatHex(hashBytes);
    }

    public void deleteRefreshTokensByMerchant(Merchant merchant) {
        refreshTokenRepository.deleteRefreshTokensByMerchant(merchant);
    }
}
