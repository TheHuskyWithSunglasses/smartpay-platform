package com.smartpay.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotNull BigDecimal amount,
        @NotNull @Size(min = 3, max = 3) String currency,
        @NotNull String idempotencyKey,
        String description,
        @NotBlank String webhookUrl
) {
}
