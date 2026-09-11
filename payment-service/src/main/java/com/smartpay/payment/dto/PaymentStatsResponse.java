package com.smartpay.payment.dto;

import com.smartpay.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.util.Map;

public record PaymentStatsResponse(BigDecimal totalVolume, Long transactionCount, Map<PaymentStatus, Long> countPerStatus) {
}
