package com.payment.payment_processing.payment.repository;

import com.payment.payment_processing.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findAllByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<Payment> findByIdAndUser_Id(Long paymentId, Long userId);

    boolean existsByIdempotencyKey(String idempotencyKey);
}