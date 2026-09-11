package com.smartpay.notification.dto.kafka;


import com.smartpay.notification.domain.PaymentStatus;

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
        OffsetDateTime occurredAt
) {
}


