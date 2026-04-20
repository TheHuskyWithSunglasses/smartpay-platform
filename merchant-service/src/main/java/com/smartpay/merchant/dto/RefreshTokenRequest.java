package com.smartpay.merchant.dto;

import jakarta.validation.constraints.NotEmpty;

public record RefreshTokenRequest(@NotEmpty String refreshToken) {
}
