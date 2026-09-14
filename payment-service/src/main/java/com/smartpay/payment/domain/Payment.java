package com.smartpay.payment.domain;

import com.smartpay.payment.domain.exception.InvalidStateTransitionException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String description;

    @Column(nullable = false)
    private String webhookUrl;

    @CreationTimestamp
    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Setter
    private OffsetDateTime processedAt;

    public void setStatus(PaymentStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new InvalidStateTransitionException(String.format("Payment status '%s' can not transition to '%s'", this.status, newStatus));
        }
        this.status = newStatus;
    }
}

