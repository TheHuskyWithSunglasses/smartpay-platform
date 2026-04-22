package com.smartpay.merchant.mapper;

import com.smartpay.merchant.domain.Merchant;
import com.smartpay.merchant.dto.MerchantResponse;
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

    public static MerchantResponse toMerchantResponse (Merchant entity) {
        return new MerchantResponse(
                entity.getId(),
                entity.getEmail(),
                entity.getBusinessName(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
