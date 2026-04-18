package com.smartpay.merchant.repository;

import com.smartpay.merchant.domain.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    Optional<Merchant> findByEmail(String email);

    boolean existsMerchantByEmail(String email);
}
