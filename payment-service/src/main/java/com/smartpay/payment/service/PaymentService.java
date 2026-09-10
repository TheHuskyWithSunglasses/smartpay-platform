package com.smartpay.payment.service;

import com.smartpay.payment.domain.Payment;
import com.smartpay.payment.domain.PaymentStatus;
import com.smartpay.payment.domain.exception.PaymentNotFoundException;
import com.smartpay.payment.dto.CreatePaymentRequest;
import com.smartpay.payment.dto.PaymentResponse;
import com.smartpay.payment.mapper.PaymentMapper;
import com.smartpay.payment.repository.PaymentRepository;
import com.smartpay.payment.specification.PaymentSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static com.smartpay.payment.mapper.PaymentMapper.toPaymentEntity;
import static com.smartpay.payment.mapper.PaymentMapper.toPaymentResponse;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentResponse createPayment(CreatePaymentRequest createPaymentRequest, UUID merchantId) {
        return paymentRepository.findByIdempotencyKey(createPaymentRequest.idempotencyKey())
                .map(PaymentMapper::toPaymentResponse)
                .orElseGet(() -> {
                    Payment payment = toPaymentEntity(createPaymentRequest, merchantId, PaymentStatus.PENDING);
                    paymentRepository.save(payment);
                    return toPaymentResponse(payment);
                });
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
}
