package com.smartpay.payment.controller;

import com.smartpay.payment.domain.PaymentStatus;
import com.smartpay.payment.dto.CreatePaymentRequest;
import com.smartpay.payment.dto.PaymentResponse;
import com.smartpay.payment.dto.PaymentStatsResponse;
import com.smartpay.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest createPaymentRequest, @RequestHeader("X-Merchant-Id") UUID merchantID) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(paymentService.createPayment(createPaymentRequest, merchantID));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable("id") UUID paymentId) {
        return ResponseEntity
                .ok()
                .body(paymentService.getPayment(paymentId));
    }

    @GetMapping
    public ResponseEntity<Page<PaymentResponse>> getPayments(@RequestHeader("X-Merchant-Id") UUID merchantID,
                                                             @RequestParam(value = "status", required = false) PaymentStatus status,
                                                             @RequestParam(value = "from", required = false) OffsetDateTime from,
                                                             @RequestParam(value = "to", required = false) OffsetDateTime to,
                                                             Pageable pageable) {
        return ResponseEntity
                .ok()
                .body(paymentService.listPayments(merchantID, status, from, to, pageable));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable("id") UUID paymentId) {
        return ResponseEntity
                .ok()
                .body(paymentService.refundPayment(paymentId));
      
    @GetMapping("/stats")
    public ResponseEntity<PaymentStatsResponse> getPaymentStats(@RequestHeader("X-Merchant-Id") UUID merchantID) {
        return ResponseEntity
                .ok()
                .body(paymentService.getStats(merchantID));
    }
}
