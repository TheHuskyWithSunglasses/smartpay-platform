package com.smartpay.merchant.controller;

import com.smartpay.merchant.dto.MerchantResponse;
import com.smartpay.merchant.dto.UpdateMerchantRequest;
import com.smartpay.merchant.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    @GetMapping("/me")
    public ResponseEntity<MerchantResponse> getCurrentMerchant() {
        return ResponseEntity
                .ok()
                .body(merchantService.getCurrentMerchant());
    }

    @PutMapping("/me")
    public ResponseEntity<MerchantResponse> updateCurrentMerchant(@Valid @RequestBody UpdateMerchantRequest request) {
        return ResponseEntity
                .ok()
                .body(merchantService.updateCurrentMerchant(request));
    }
}
