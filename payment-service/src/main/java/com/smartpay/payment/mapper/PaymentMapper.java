package com.smartpay.payment.mapper;

import com.smartpay.payment.domain.Payment;
import com.smartpay.payment.domain.PaymentStatus;
import com.smartpay.payment.dto.CreatePaymentRequest;
import com.smartpay.payment.dto.PaymentResponse;
import com.smartpay.payment.dto.kafka.PaymentEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.UUID;

public class PaymentMapper {

    private  PaymentMapper() {}

    public static PaymentResponse toPaymentResponse (Payment entity) {
        return new PaymentResponse(
                entity.getId(),
                entity.getStatus(),
                toDecimalAmount(entity.getAmount()),
                entity.getCurrency(),
                entity.getDescription(),
                entity.getIdempotencyKey(),
                entity.getWebhookUrl(),
                entity.getCreatedAt(),
                entity.getProcessedAt()
        );
    }

    public static Payment toPaymentEntity (CreatePaymentRequest record, UUID merchantId, PaymentStatus paymentStatus) {
        return Payment.builder()
                .amount(toLongAmount(record.amount()))
                .currency(record.currency())
                .idempotencyKey(record.idempotencyKey())
                .description(record.description())
                .status(paymentStatus)
                .merchantId(merchantId)
                .webhookUrl(record.webhookUrl())
                .build();
    }

    public static PaymentEvent toPaymentEvent (Payment entity, PaymentStatus previousStatus) {
        return new PaymentEvent(
                UUID.randomUUID(),
                entity.getId(),
                entity.getMerchantId(),
                previousStatus,
                entity.getStatus(),
                toDecimalAmount(entity.getAmount()),
                entity.getCurrency(),
                OffsetDateTime.now(),
                entity.getWebhookUrl()
        );
    }

    public static BigDecimal toDecimalAmount(Long amount) {
        return BigDecimal.valueOf(amount).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
    }

    public static Long toLongAmount(BigDecimal amount) {
        return amount.multiply(new BigDecimal(100)).longValue();
    }
}
