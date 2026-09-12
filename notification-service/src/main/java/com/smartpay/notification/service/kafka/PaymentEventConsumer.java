package com.smartpay.notification.service.kafka;

import com.smartpay.notification.domain.PaymentStatus;
import com.smartpay.notification.dto.kafka.PaymentEvent;
import com.smartpay.notification.service.WebhookDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentEventConsumer {
    private final WebhookDeliveryService webhookDeliveryService;

    @KafkaListener(
            topics = "payment-events",
            groupId = "notification-service"
    )
    public void consume(PaymentEvent event) {
        // Send notification
        if (event.status() == PaymentStatus.COMPLETED || event.status() == PaymentStatus.FAILED) {
            webhookDeliveryService.sendNotification(event);
        }
    }
}
