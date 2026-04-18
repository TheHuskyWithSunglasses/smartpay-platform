package com.smartpay.merchant.mapper;

import com.smartpay.merchant.domain.Merchant;
import com.smartpay.merchant.dto.RegisterMerchantResponse;

public class MerchantMapper {

    private MerchantMapper () {}

    public static RegisterMerchantResponse toMerchantRecord (Merchant entity) {
        return new RegisterMerchantResponse(
                entity.getId(),
                entity.getEmail(),
                entity.getBusinessName(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
