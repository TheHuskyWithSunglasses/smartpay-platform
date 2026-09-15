package com.smartpay.notification.repository;

import com.smartpay.notification.domain.WebhookDelivery;
import com.smartpay.notification.domain.WebhookStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {
    boolean existsByPaymentIdAndStatus(UUID paymentId, WebhookStatus status);
}
