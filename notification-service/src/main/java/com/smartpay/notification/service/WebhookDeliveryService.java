package com.smartpay.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpay.notification.domain.WebhookDelivery;
import com.smartpay.notification.domain.WebhookStatus;
import com.smartpay.notification.dto.kafka.PaymentEvent;
import com.smartpay.notification.mapper.WebhookDeliveryMapper;
import com.smartpay.notification.repository.WebhookDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookDeliveryService {
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public void sendNotification(PaymentEvent event) {
        if (webhookDeliveryRepository.existsByPaymentIdAndStatus(event.paymentId(), WebhookStatus.SUCCESS)) {
            log.info("Webhook already delivered for payment: {}", event.paymentId());
            return;
        }

        String eventPayload;
        try {
            eventPayload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        WebhookDelivery webhookDelivery = WebhookDeliveryMapper.toWebhookDelivery(event, eventPayload);
        webhookDeliveryRepository.save(webhookDelivery);

        tryAndRetry(webhookDelivery, webhookDelivery.getWebhookUrl(), eventPayload);
    }

    private void tryAndRetry(WebhookDelivery webhookDelivery, String url, String eventPayload) {
        int[] delays = {1000, 5000, 25000};
        int maxAttempts = 3;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // update attemptCount and lastAttemptedAt
            webhookDelivery.setAttemptCount(attempt + 1);
            webhookDelivery.setLastAttemptedAt(OffsetDateTime.now());
            webhookDeliveryRepository.save(webhookDelivery);

            // try delivery
            // if success → SUCCESS, save, return
            if (attemptDelivery(url, eventPayload)) {
                webhookDelivery.setStatus(WebhookStatus.SUCCESS);
                webhookDeliveryRepository.save(webhookDelivery);
                log.info("Webhook delivered successfully to: {} for payment: {}", url, webhookDelivery.getPaymentId());
                return;
            } else {
                // if failed and not last attempt → wait delays[attempt]
                if (attempt < maxAttempts - 1) {
                    try {
                        Thread.sleep(delays[attempt]);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    // if failed and last attempt → DEAD, save
                    log.error("Webhook delivery DEAD after {} attempts for payment: {}", maxAttempts, webhookDelivery.getPaymentId());
                    webhookDelivery.setStatus(WebhookStatus.DEAD);
                    webhookDeliveryRepository.save(webhookDelivery);
                }
            }
        }
    }

    private boolean attemptDelivery(String url, String eventPayload) {
        log.info("Attempting delivery to: {}", url);
        try {
            restClient.post()
                    .uri(url)
                    .body(eventPayload)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.error("Webhook delivery failed: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Webhook delivery failed unexpected: {} - {}", e.getClass().getName(), e.getMessage());
            return false;
        }
    }
}
