package com.smartpay.payment.service.kafka;

import com.smartpay.payment.dto.PaymentResponse;
import com.smartpay.payment.dto.kafka.PaymentEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventProducer {
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public PaymentEventProducer(
            KafkaTemplate<String, PaymentEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(PaymentEvent event) {
        kafkaTemplate.send(
                "payment-events",
                event.paymentId().toString(),
                event
        );
    }
}
