package com.smartpay.payment.service;

import com.smartpay.payment.domain.Payment;
import com.smartpay.payment.domain.PaymentStatus;
import com.smartpay.payment.domain.exception.InvalidStateTransitionException;
import com.smartpay.payment.domain.exception.PaymentNotFoundException;
import com.smartpay.payment.dto.CreatePaymentRequest;
import com.smartpay.payment.dto.PaymentResponse;
import com.smartpay.payment.repository.PaymentRepository;
import com.smartpay.payment.service.kafka.PaymentEventProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @InjectMocks
    private PaymentService paymentService;

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
}