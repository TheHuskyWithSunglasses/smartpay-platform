package com.smartpay.payment.service;

import com.smartpay.payment.domain.Payment;
import com.smartpay.payment.domain.PaymentStatus;
import com.smartpay.payment.domain.exception.PaymentNotFoundException;
import com.smartpay.payment.dto.CallbackRequest;
import com.smartpay.payment.dto.CreatePaymentRequest;
import com.smartpay.payment.dto.PaymentResponse;
import com.smartpay.payment.dto.PaymentStatsResponse;
import com.smartpay.payment.dto.kafka.PaymentEvent;
import com.smartpay.payment.mapper.PaymentMapper;
import com.smartpay.payment.repository.PaymentRepository;
import com.smartpay.payment.service.kafka.PaymentEventProducer;
import com.smartpay.payment.specification.PaymentSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.smartpay.payment.mapper.PaymentMapper.*;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final IdempotencyService idempotencyService;

    public PaymentResponse createPayment(CreatePaymentRequest createPaymentRequest, UUID merchantId) {
        String idempotencyKey = createPaymentRequest.idempotencyKey();

        // Check Redis cache first
        Optional<PaymentResponse> cached = idempotencyService.getIfPresent(idempotencyKey);
        if (cached.isPresent()) {
            return cached.get();
        }

        // Check database
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            PaymentResponse response = PaymentMapper.toPaymentResponse(existing.get());
            idempotencyService.store(idempotencyKey, response);
            return response;
        }

        // Create new payment
        Payment payment = toPaymentEntity(createPaymentRequest, merchantId, PaymentStatus.PENDING);
        paymentRepository.save(payment);
        paymentEventProducer.publish(toPaymentEvent(payment, null));
        PaymentResponse response = toPaymentResponse(payment);
        idempotencyService.store(idempotencyKey, response);
        return response;
    }

    public PaymentResponse getPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .map(PaymentMapper::toPaymentResponse)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId.toString()));
    }

    public Page<PaymentResponse> listPayments(UUID merchantId, PaymentStatus paymentStatus, OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        Specification<Payment> spec = PaymentSpecifications.hasMerchantId(merchantId);

        if (paymentStatus != null) {
            spec = spec.and(PaymentSpecifications.hasStatus(paymentStatus));
        }

        if (from != null) {
            spec = spec.and(PaymentSpecifications.createdAfter(from));
        }

        if (to != null) {
            spec = spec.and(PaymentSpecifications.createdBefore(to));
        }

        return paymentRepository.findAll(spec, pageable).map(PaymentMapper::toPaymentResponse);
    }

    public PaymentResponse refundPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .map(payment -> {
                    PaymentStatus paymentStatus = payment.getStatus();
                    payment.setStatus(PaymentStatus.REFUNDED);
                    paymentRepository.save(payment);
                    PaymentEvent paymentEvent = toPaymentEvent(payment, paymentStatus);
                    paymentEventProducer.publish(paymentEvent);

                    return toPaymentResponse(payment);
                })
                .orElseThrow(() -> new PaymentNotFoundException(paymentId.toString()));
    }
    
    public PaymentStatsResponse getStats(UUID merchantId) {
        List<Object[]> results = paymentRepository.countByMerchantIdGroupByStatus(merchantId);
        Long totalTransactions = paymentRepository.getTransactionsCount(merchantId);
        Long totalVolume = paymentRepository.getTotalVolume(merchantId);
        BigDecimal volume = totalVolume != null ? PaymentMapper.toDecimalAmount(totalVolume) : BigDecimal.ZERO;

        Map<PaymentStatus, Long> countPerStatus = results.stream()
                .collect(Collectors.toMap(
                        row -> (PaymentStatus) row[0],
                        row -> (Long) row[1]
                ));

        return new PaymentStatsResponse(
                volume,
                totalTransactions,
                countPerStatus
        );
    }

    public PaymentResponse callbackStatusUpdate(UUID paymentId, CallbackRequest callbackRequest) {
        return paymentRepository.findById(paymentId)
                .map(payment ->  {
                    PaymentStatus previousPaymentStatus = payment.getStatus();
                    payment.setStatus(callbackRequest.status());
                    paymentRepository.save(payment);
                    paymentEventProducer.publish(toPaymentEvent(payment, previousPaymentStatus));
                    return toPaymentResponse(payment);
                }).orElseThrow(() -> new PaymentNotFoundException(paymentId.toString()));
    }
}
