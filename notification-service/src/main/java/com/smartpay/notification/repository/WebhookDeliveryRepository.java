package com.smartpay.notification.repository;

import com.smartpay.notification.domain.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {
}
