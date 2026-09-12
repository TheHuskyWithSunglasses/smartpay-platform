package com.smartpay.notification.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "webhook_deliveries")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    @Column(nullable = false)
    private UUID paymentId;

    @Column(nullable = false)
    private String webhookUrl;

    private String payload;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WebhookStatus status;

    @Setter
    private int attemptCount;

    @Setter
    private OffsetDateTime lastAttemptedAt;

    @CreationTimestamp
    @Column(nullable = false)
    private OffsetDateTime createdAt;
}
