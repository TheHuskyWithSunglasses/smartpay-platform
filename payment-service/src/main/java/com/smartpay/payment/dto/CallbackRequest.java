package com.smartpay.payment.dto;

import com.smartpay.payment.domain.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public record CallbackRequest(@NotNull PaymentStatus status) {
}
