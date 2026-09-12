package com.smartpay.notification.service.kafka;

import com.smartpay.notification.dto.kafka.PaymentEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventConsumer {

    @KafkaListener(
            topics = "payment-events",
            groupId = "notification-service"
    )
    public void consume(PaymentEvent event) {

        System.out.println(
                "Payment completed: " + event.paymentId()
        );

        // Send notification
    }
}
