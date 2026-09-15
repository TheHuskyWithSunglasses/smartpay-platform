package com.smartpay.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpay.payment.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration TTL = Duration.ofHours(24);

    public Optional<PaymentResponse> getIfPresent(String idempotencyKey) {
        String cached = redisTemplate.opsForValue().get(idempotencyKey);
        if (cached == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(cached, PaymentResponse.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void store(String idempotencyKey, PaymentResponse response) {
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(idempotencyKey, responseJson, TTL);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
