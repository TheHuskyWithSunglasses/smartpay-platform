package com.smartpay.payment.dto.kafka;

import com.smartpay.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentEvent(
        UUID eventId,
        UUID paymentId,
        UUID merchantId,
        PaymentStatus previousStatus,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        OffsetDateTime occurredAt,
        String webhookUrl
) {
}


