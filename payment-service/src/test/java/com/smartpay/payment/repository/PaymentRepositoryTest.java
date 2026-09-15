package com.smartpay.payment.repository;

import com.smartpay.payment.domain.Payment;
import com.smartpay.payment.domain.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.smartpay.payment.specification.PaymentSpecifications.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private PaymentRepository paymentRepository;

    private UUID merchantId;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        merchantId = UUID.randomUUID();
    }

    private Payment buildPayment(String idempotencyKey, PaymentStatus status, long amount) {
        return Payment.builder()
                .merchantId(merchantId)
                .amount(amount)
                .currency("EUR")
                .idempotencyKey(idempotencyKey)
                .status(status)
                .webhookUrl("https://example.com/webhook")
                .build();
    }

    @Test
    void findByIdempotencyKey_whenExists_returnsPayment() {
        paymentRepository.save(buildPayment("key-001", PaymentStatus.PENDING, 1000L));

        Optional<Payment> result = paymentRepository.findByIdempotencyKey("key-001");

        assertTrue(result.isPresent());
        assertEquals("key-001", result.get().getIdempotencyKey());
    }

    @Test
    void findByIdempotencyKey_whenNotExists_returnsEmpty() {
        Optional<Payment> result = paymentRepository.findByIdempotencyKey("non-existent-key");

        assertFalse(result.isPresent());
    }

    @Test
    void findAll_withMerchantIdSpec_returnsOnlyMerchantPayments() {
        paymentRepository.save(buildPayment("key-001", PaymentStatus.PENDING, 1000L));
        paymentRepository.save(buildPayment("key-002", PaymentStatus.COMPLETED, 2000L));

        Payment otherMerchantPayment = Payment.builder()
                .merchantId(UUID.randomUUID())
                .amount(500L)
                .currency("EUR")
                .idempotencyKey("key-003")
                .status(PaymentStatus.PENDING)
                .webhookUrl("https://example.com/webhook")
                .build();
        paymentRepository.save(otherMerchantPayment);

        Specification<Payment> spec = hasMerchantId(merchantId);
        List<Payment> results = paymentRepository.findAll(spec);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(p -> p.getMerchantId().equals(merchantId)));
    }

    @Test
    void findAll_withStatusSpec_returnsOnlyMatchingStatus() {
        paymentRepository.save(buildPayment("key-001", PaymentStatus.PENDING, 1000L));
        paymentRepository.save(buildPayment("key-002", PaymentStatus.COMPLETED, 2000L));
        paymentRepository.save(buildPayment("key-003", PaymentStatus.FAILED, 3000L));

        Specification<Payment> spec = hasMerchantId(merchantId).and(hasStatus(PaymentStatus.COMPLETED));
        List<Payment> results = paymentRepository.findAll(spec);

        assertEquals(1, results.size());
        assertEquals(PaymentStatus.COMPLETED, results.get(0).getStatus());
    }

    @Test
    void getTotalVolume_returnsCorrectSum() {
        paymentRepository.save(buildPayment("key-001", PaymentStatus.COMPLETED, 1000L));
        paymentRepository.save(buildPayment("key-002", PaymentStatus.COMPLETED, 2000L));
        paymentRepository.save(buildPayment("key-003", PaymentStatus.FAILED, 500L));

        Long totalVolume = paymentRepository.getTotalVolume(merchantId);

        assertEquals(3500L, totalVolume);
    }

    @Test
    void getTransactionsCount_returnsCorrectCount() {
        paymentRepository.save(buildPayment("key-001", PaymentStatus.PENDING, 1000L));
        paymentRepository.save(buildPayment("key-002", PaymentStatus.COMPLETED, 2000L));
        paymentRepository.save(buildPayment("key-003", PaymentStatus.FAILED, 500L));

        Long count = paymentRepository.getTransactionsCount(merchantId);

        assertEquals(3L, count);
    }

    @Test
    void countByMerchantIdGroupByStatus_returnsCorrectGrouping() {
        paymentRepository.save(buildPayment("key-001", PaymentStatus.PENDING, 1000L));
        paymentRepository.save(buildPayment("key-002", PaymentStatus.PENDING, 2000L));
        paymentRepository.save(buildPayment("key-003", PaymentStatus.COMPLETED, 500L));

        List<Object[]> results = paymentRepository.countByMerchantIdGroupByStatus(merchantId);

        assertEquals(2, results.size());
        results.forEach(row -> {
            PaymentStatus status = (PaymentStatus) row[0];
            Long count = (Long) row[1];
            if (status == PaymentStatus.PENDING) {
                assertEquals(2L, count);
            } else if (status == PaymentStatus.COMPLETED) {
                assertEquals(1L, count);
            }
        });
    }
}