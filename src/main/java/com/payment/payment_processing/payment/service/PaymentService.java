package com.payment.payment_processing.payment.service;

import com.payment.payment_processing.payment.dto.PaymentResponse;
import com.payment.payment_processing.payment.entity.Payment;
import com.payment.payment_processing.payment.entity.PaymentStatus;
import com.payment.payment_processing.payment.repository.PaymentRepository;
import com.payment.payment_processing.user.entity.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import com.payment.payment_processing.payment.event.PaymentEvent;
import com.payment.payment_processing.payment.kafka.PaymentProducer;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProducer paymentProducer;
    private final RedisTemplate<String, PaymentResponse> redisTemplate;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentProducer paymentProducer,
            RedisTemplate<String, PaymentResponse> redisTemplate) {

        this.paymentRepository = paymentRepository;
        this.paymentProducer = paymentProducer;
        this.redisTemplate = redisTemplate;
    }

    public Payment createPayment(
            User user,
            String idempotencyKey,
            BigDecimal amount) {

        Optional<Payment> existingPayment =
                paymentRepository.findByIdempotencyKey(idempotencyKey);

        if (existingPayment.isPresent()) {
            return existingPayment.get();
        }

        try {
            Payment payment = new Payment();
            payment.setUser(user);
            payment.setIdempotencyKey(idempotencyKey);
            payment.setAmount(amount);
            payment.setCurrency("INR");
            payment.setStatus(PaymentStatus.PENDING);

            Payment savedPayment = paymentRepository.save(payment);

            PaymentEvent event = new PaymentEvent(
                    savedPayment.getId(),
                    user.getId(),
                    savedPayment.getAmount(),
                    savedPayment.getCurrency()
            );

            paymentProducer.sendPaymentCreatedEvent(event);

            return savedPayment;

        } catch (DataIntegrityViolationException exception) {

            return paymentRepository
                    .findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Payment could not be created"
                            ));
        }
    }

    public PaymentResponse getPaymentById(Long paymentId) {

        String cacheKey = "payment:" + paymentId;

        PaymentResponse cachedPayment = redisTemplate
                .opsForValue()
                .get(cacheKey);

        if (cachedPayment != null) {
            System.out.println("Payment found in Redis");
            return cachedPayment;
        }

        System.out.println("Payment not found in Redis. Fetching from database.");

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        PaymentResponse response = new PaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getCreatedAt()
        );

        redisTemplate
                .opsForValue()
                .set(cacheKey, response);

        System.out.println("Payment stored in Redis");

        return response;
    }

    public void clearPaymentCache(Long paymentId) {

        String cacheKey = "payment:" + paymentId;

        redisTemplate.delete(cacheKey);

        System.out.println("Redis cache cleared for payment: " + paymentId);
    }
}