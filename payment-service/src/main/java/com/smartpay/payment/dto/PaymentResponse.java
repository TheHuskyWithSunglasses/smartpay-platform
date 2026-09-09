package com.smartpay.payment.dto;

import com.smartpay.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(UUID id, PaymentStatus status, BigDecimal amount, String currency, String description, String idempotencyKey, OffsetDateTime createdAt, OffsetDateTime processedAt) {
}
