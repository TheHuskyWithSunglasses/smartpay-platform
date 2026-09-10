package com.smartpay.payment.specification;

import com.smartpay.payment.domain.Payment;
import com.smartpay.payment.domain.PaymentStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.UUID;

public class PaymentSpecifications {
    private PaymentSpecifications() {}

    public static Specification<Payment> hasMerchantId(UUID merchantId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("merchantId"), merchantId);
    }

    public static Specification<Payment> hasStatus(PaymentStatus status) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Payment> createdAfter(OffsetDateTime date) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThan(root.get("createdAt"), date);
    }

    public static Specification<Payment> createdBefore(OffsetDateTime date) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThan(root.get("createdAt"), date);
    }

}
