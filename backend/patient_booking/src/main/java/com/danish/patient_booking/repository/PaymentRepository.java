package com.danish.patient_booking.repository;

import com.danish.patient_booking.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// PaymentRepository.java
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Used in cancelBooking() to update payment status to REFUNDED
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
}
