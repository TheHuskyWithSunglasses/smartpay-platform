package com.smartpay.notification.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpay.notification.domain.WebhookDelivery;
import com.smartpay.notification.domain.WebhookStatus;
import com.smartpay.notification.dto.kafka.PaymentEvent;

import java.time.OffsetDateTime;

public class WebhookDeliveryMapper {
    private WebhookDeliveryMapper() {}

    public static WebhookDelivery toWebhookDelivery(PaymentEvent event, String eventJson) {
        return WebhookDelivery.builder()
                .webhookUrl(event.webhookUrl())
                .paymentId(event.paymentId())
                .merchantId(event.merchantId())
                .status(WebhookStatus.PENDING)
                .attemptCount(0)
                .lastAttemptedAt(OffsetDateTime.now())
                .payload(eventJson)
                .build();
    }
}
