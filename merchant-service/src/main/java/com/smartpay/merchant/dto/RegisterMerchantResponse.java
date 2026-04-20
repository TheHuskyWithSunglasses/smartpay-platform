package com.smartpay.merchant.dto;

import com.smartpay.merchant.domain.MerchantStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RegisterMerchantResponse (UUID id, String email, String businessName, MerchantStatus status, OffsetDateTime createdAt) {
}
