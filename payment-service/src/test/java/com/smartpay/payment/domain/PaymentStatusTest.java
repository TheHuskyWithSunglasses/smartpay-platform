package com.smartpay.payment.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentStatusTest {

    @Test
    void processingCanTransitionToCompleted() {
        assertTrue(PaymentStatus.PROCESSING.canTransitionTo(PaymentStatus.COMPLETED));
    }

    @Test
    void processingCanTransitionToFailed() {
        assertTrue(PaymentStatus.PROCESSING.canTransitionTo(PaymentStatus.FAILED));
    }

    @Test
    void completedCanTransitionToRefunded() {
        assertTrue(PaymentStatus.COMPLETED.canTransitionTo(PaymentStatus.REFUNDED));
    }

    @Test
    void pendingCannotTransitionToCompleted() {
        assertFalse(PaymentStatus.PENDING.canTransitionTo(PaymentStatus.COMPLETED));
    }

    @Test
    void completedCannotTransitionToProcessing() {
        assertFalse(PaymentStatus.COMPLETED.canTransitionTo(PaymentStatus.PROCESSING));
    }

    @Test
    void failedCannotTransitionToAnything() {
        assertFalse(PaymentStatus.FAILED.canTransitionTo(PaymentStatus.PROCESSING));
        assertFalse(PaymentStatus.FAILED.canTransitionTo(PaymentStatus.COMPLETED));
        assertFalse(PaymentStatus.FAILED.canTransitionTo(PaymentStatus.REFUNDED));
    }

    @Test
    void refundedCannotTransitionToAnything() {
        assertFalse(PaymentStatus.REFUNDED.canTransitionTo(PaymentStatus.PROCESSING));
        assertFalse(PaymentStatus.REFUNDED.canTransitionTo(PaymentStatus.COMPLETED));
        assertFalse(PaymentStatus.REFUNDED.canTransitionTo(PaymentStatus.FAILED));
    }
}