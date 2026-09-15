package com.smartpay.payment.service;

import com.smartpay.payment.domain.Payment;
import com.smartpay.payment.domain.PaymentStatus;
import com.smartpay.payment.domain.exception.InvalidStateTransitionException;
import com.smartpay.payment.domain.exception.PaymentNotFoundException;
import com.smartpay.payment.dto.CallbackRequest;
import com.smartpay.payment.dto.CreatePaymentRequest;
import com.smartpay.payment.dto.PaymentResponse;
import com.smartpay.payment.dto.PaymentStatsResponse;
import com.smartpay.payment.repository.PaymentRepository;
import com.smartpay.payment.service.kafka.PaymentEventProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private IdempotencyService idempotencyService;

    @Test
    void createPayment_whenIdempotencyKeyExists_returnsExistingPayment() {
        UUID merchantId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .idempotencyKey("idempotencyKey")
                .status(PaymentStatus.COMPLETED)
                .merchantId(merchantId)
                .amount(1000L)
                .currency("EUR")
                .webhookUrl("https://example.com/webhook")
                .build();

        when(idempotencyService.getIfPresent("idempotencyKey")).thenReturn(Optional.empty());
        when(paymentRepository.findByIdempotencyKey("idempotencyKey"))
                .thenReturn(Optional.of(payment));

        CreatePaymentRequest request = new CreatePaymentRequest(
                BigDecimal.valueOf(10.00),
                "EUR",
                "idempotencyKey",
                "test payment",
                "https://example.com/webhook"
        );

        PaymentResponse response = paymentService.createPayment(request, merchantId);

        verify(paymentRepository, never()).save(any());
        assertEquals(PaymentStatus.COMPLETED, response.status());
    }

    @Test
    void createPayment_whenIdempotencyKeyDoesNotExist_savesNewPayment() {
        UUID merchantId = UUID.randomUUID();
        when(idempotencyService.getIfPresent("new-key")).thenReturn(Optional.empty());
        when(paymentRepository.findByIdempotencyKey("new-key"))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreatePaymentRequest request = new CreatePaymentRequest(
                BigDecimal.valueOf(10.00),
                "EUR",
                "new-key",
                "test payment",
                "https://example.com/webhook"
        );

        PaymentResponse response = paymentService.createPayment(request, merchantId);

        verify(paymentRepository, times(1)).save(any());
        assertEquals(PaymentStatus.PENDING, response.status());
    }

    @Test
    void getPayment_whenNotFound_throwsPaymentNotFoundException() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class,
                () -> paymentService.getPayment(paymentId));
    }

    @Test
    void refundPayment_whenPaymentIsCompleted_transitionsToRefunded() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .idempotencyKey("key")
                .status(PaymentStatus.COMPLETED)
                .merchantId(UUID.randomUUID())
                .amount(1000L)
                .currency("EUR")
                .webhookUrl("https://example.com/webhook")
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.refundPayment(paymentId);

        assertEquals(PaymentStatus.REFUNDED, response.status());
        verify(paymentEventProducer, times(1)).publish(any());
    }

    @Test
    void refundPayment_whenPaymentIsPending_throwsInvalidStateTransitionException() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .idempotencyKey("key")
                .status(PaymentStatus.PENDING)
                .merchantId(UUID.randomUUID())
                .amount(1000L)
                .currency("EUR")
                .webhookUrl("https://example.com/webhook")
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThrows(InvalidStateTransitionException.class,
                () -> paymentService.refundPayment(paymentId));
    }

    @Test
    void createPayment_whenCachedInRedis_returnsCachedResponse() {
        PaymentResponse paymentResponse = new PaymentResponse(
                UUID.randomUUID(),
                PaymentStatus.PENDING,
                BigDecimal.TEN,
                "EUR",
                "Test",
                "key",
                "https://example.com/webhook",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        CreatePaymentRequest createPaymentRequest = new CreatePaymentRequest(
                paymentResponse.amount(),
                paymentResponse.currency(),
                paymentResponse.idempotencyKey(),
                paymentResponse.description(),
                paymentResponse.webhookUrl());

        when(idempotencyService.getIfPresent("key")).thenReturn(Optional.of(paymentResponse));

        PaymentResponse payment = paymentService.createPayment(createPaymentRequest, UUID.randomUUID());

        assertEquals(payment, paymentResponse);
        verify(paymentRepository, never()).save(any());
        verify(paymentEventProducer, never()).publish(any());
    }

    @Test
    void getStats_returnsAggregatedStats() {
        UUID merchantId = UUID.randomUUID();

        // Mock the three repository calls
        List<Object[]> statsResults = List.of(
                new Object[]{PaymentStatus.COMPLETED, 5L},
                new Object[]{PaymentStatus.PENDING, 2L},
                new Object[]{PaymentStatus.FAILED, 1L}
        );
        when(paymentRepository.countByMerchantIdGroupByStatus(merchantId))
                .thenReturn(statsResults);

        when(paymentRepository.getTransactionsCount(merchantId)).thenReturn(8L);
        when(paymentRepository.getTotalVolume(merchantId)).thenReturn(50000L); // in cents = €500

        PaymentStatsResponse stats = paymentService.getStats(merchantId);

        // Verify all three repository methods were called once
        verify(paymentRepository, times(1)).countByMerchantIdGroupByStatus(merchantId);
        verify(paymentRepository, times(1)).getTransactionsCount(merchantId);
        verify(paymentRepository, times(1)).getTotalVolume(merchantId);

        // Assert the response contains correct data
        assertEquals(0, stats.totalVolume().compareTo(BigDecimal.valueOf(500)));
        assertEquals(8L, stats.transactionCount());
        assertEquals(5L, stats.countPerStatus().get(PaymentStatus.COMPLETED));
        assertEquals(2L, stats.countPerStatus().get(PaymentStatus.PENDING));
        assertEquals(1L, stats.countPerStatus().get(PaymentStatus.FAILED));
    }

    @Test
    void getStats_whenTotalVolumeIsNull_returnsZeroVolume() {
        UUID merchantId = UUID.randomUUID();

        // Simulate no transactions
        when(paymentRepository.countByMerchantIdGroupByStatus(merchantId))
                .thenReturn(List.of());
        when(paymentRepository.getTransactionsCount(merchantId)).thenReturn(0L);
        when(paymentRepository.getTotalVolume(merchantId)).thenReturn(null); // null check

        PaymentStatsResponse stats = paymentService.getStats(merchantId);

        assertEquals(BigDecimal.ZERO, stats.totalVolume());
        assertEquals(0L, stats.transactionCount());
        assertTrue(stats.countPerStatus().isEmpty());
    }

    @Test
    void callbackStatusUpdate_whenPaymentExists_updatesStatusAndPublishesEvent() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .idempotencyKey("key")
                .status(PaymentStatus.PROCESSING)
                .merchantId(UUID.randomUUID())
                .amount(1000L)
                .currency("EUR")
                .webhookUrl("https://example.com/webhook")
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CallbackRequest callbackRequest = new CallbackRequest(PaymentStatus.COMPLETED);

        PaymentResponse response = paymentService.callbackStatusUpdate(paymentId, callbackRequest);

        // Verify status was updated to COMPLETED
        assertEquals(PaymentStatus.COMPLETED, response.status());

        // Verify save was called (status was persisted)
        verify(paymentRepository, times(1)).save(any());

        // Verify event was published with the transition
        verify(paymentEventProducer, times(1)).publish(any());
    }

    @Test
    void callbackStatusUpdate_whenPaymentNotFound_throwsPaymentNotFoundException() {
        UUID paymentId = UUID.randomUUID();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        CallbackRequest callbackRequest = new CallbackRequest(PaymentStatus.COMPLETED);

        assertThrows(PaymentNotFoundException.class,
                () -> paymentService.callbackStatusUpdate(paymentId, callbackRequest));

        // Verify no save or event publish happened
        verify(paymentRepository, never()).save(any());
        verify(paymentEventProducer, never()).publish(any());
    }
}