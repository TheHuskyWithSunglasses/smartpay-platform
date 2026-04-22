package com.smartpay.merchant.service;

import com.smartpay.merchant.domain.Merchant;
import com.smartpay.merchant.domain.MerchantStatus;
import com.smartpay.merchant.domain.exception.EmailAlreadyExistsException;
import com.smartpay.merchant.dto.MerchantResponse;
import com.smartpay.merchant.dto.RegisterMerchantRequest;
import com.smartpay.merchant.dto.RegisterMerchantResponse;
import com.smartpay.merchant.dto.UpdateMerchantRequest;
import com.smartpay.merchant.mapper.MerchantMapper;
import com.smartpay.merchant.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterMerchantResponse registerMerchant(RegisterMerchantRequest request) {
        if (merchantRepository.existsMerchantByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email address: " + request.email() + " is already registered");
        }

        Merchant merchant = new Merchant();
        merchant.setEmail(request.email());
        merchant.setPasswordHash(passwordEncoder.encode(request.password()));
        merchant.setBusinessName(request.businessName());
        merchant.setStatus(MerchantStatus.PENDING);

        merchantRepository.save(merchant);

        return MerchantMapper.toMerchantRecord(merchant);
    }

    public MerchantResponse getCurrentMerchant() {
        Merchant merchant = (Merchant) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return MerchantMapper.toMerchantResponse(merchant);
    }

    public MerchantResponse updateCurrentMerchant(UpdateMerchantRequest request) {
        Merchant merchant = (Merchant) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        merchant.setBusinessName(request.businessName());
        merchantRepository.save(merchant);

        return MerchantMapper.toMerchantResponse(merchant);
    }
}
