package com.smartpay.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpay.notification.domain.PaymentStatus;
import com.smartpay.notification.domain.WebhookStatus;
import com.smartpay.notification.dto.kafka.PaymentEvent;
import com.smartpay.notification.repository.WebhookDeliveryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookDeliveryServiceTest {

    @Mock
    private WebhookDeliveryRepository webhookDeliveryRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RestClient restClient;

    @InjectMocks
    private WebhookDeliveryService webhookDeliveryService;

    private PaymentEvent buildEvent() {
        return new PaymentEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                PaymentStatus.PENDING,
                PaymentStatus.COMPLETED,
                BigDecimal.valueOf(10.00),
                "EUR",
                OffsetDateTime.now(),
                "https://example.com/webhook"
        );
    }

    @Test
    void sendNotification_whenDeliverySucceeds_setsStatusSuccess() throws Exception {
        PaymentEvent event = buildEvent();
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.body(anyString())).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(null);
        when(webhookDeliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        webhookDeliveryService.sendNotification(event);

        verify(webhookDeliveryRepository, atLeastOnce()).save(argThat(
                delivery -> delivery.getStatus() == WebhookStatus.SUCCESS
        ));
    }

    @Test
    void sendNotification_whenAllAttemptsFaile_setsStatusDead() throws Exception {
        PaymentEvent event = buildEvent();
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);

        when(restClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.body(anyString())).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenThrow(new RestClientException("connection refused"));
        when(webhookDeliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        webhookDeliveryService.sendNotification(event);

        verify(webhookDeliveryRepository, atLeastOnce()).save(argThat(
                delivery -> delivery.getStatus() == WebhookStatus.DEAD
        ));
    }
}