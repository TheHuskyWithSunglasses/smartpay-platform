package com.smartpay.payment.repository;

import com.smartpay.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT SUM(p.amount) FROM Payment AS p WHERE p.merchantId = :merchantId")
    Long getTotalVolume(@Param("merchantId") UUID merchantId);

    @Query("SELECT COUNT(p) AS totalTransactions FROM Payment AS p WHERE p.merchantId = :merchantId")
    Long getTransactionsCount(@Param("merchantId") UUID merchantId);

    @Query("SELECT p.status, COUNT(p) AS totalPayments FROM Payment AS p WHERE p.merchantId = :merchantId GROUP BY p.status")
    List<Object[]> countByMerchantIdGroupByStatus(@Param("merchantId") UUID merchantId);
}
