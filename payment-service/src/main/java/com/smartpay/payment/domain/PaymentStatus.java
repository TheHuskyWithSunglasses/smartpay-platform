package com.smartpay.payment.domain;

public enum PaymentStatus {
    PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED;

    public boolean canTransitionTo(PaymentStatus next) {
        return switch (this) {
            case PENDING -> next == PROCESSING;
            case PROCESSING -> next == COMPLETED || next == FAILED;
            case COMPLETED -> next == REFUNDED;
            case FAILED, REFUNDED -> false;
        };
    }
}
