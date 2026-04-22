package com.smartpay.merchant.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateMerchantRequest(@NotBlank String businessName) {
}
