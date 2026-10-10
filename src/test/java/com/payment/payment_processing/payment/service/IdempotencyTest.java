package com.payment.payment_processing.payment.service;

import com.payment.payment_processing.payment.entity.Payment;
import com.payment.payment_processing.payment.entity.PaymentStatus;
import com.payment.payment_processing.payment.event.PaymentEvent;
import com.payment.payment_processing.payment.kafka.PaymentProducer;
import com.payment.payment_processing.payment.repository.PaymentRepository;
import com.payment.payment_processing.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentProducer paymentProducer;

    @Mock
    private org.springframework.data.redis.core.RedisTemplate<String, com.payment.payment_processing.payment.dto.PaymentResponse> redisTemplate;

    @InjectMocks
    private PaymentService paymentService;

    private User testUser;
    private Payment existingPayment;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");

        existingPayment = new Payment();
        existingPayment.setId(1L);
        existingPayment.setUser(testUser);
        existingPayment.setIdempotencyKey("unique-key-123");
        existingPayment.setAmount(new BigDecimal("100.00"));
        existingPayment.setCurrency("INR");
        existingPayment.setStatus(PaymentStatus.SUCCESS);
        existingPayment.setCreatedAt(LocalDateTime.now());
        existingPayment.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void idempotencyKey_MultipleCallsWithSameKey_ReturnsSamePayment() {
        String idempotencyKey = "unique-key-123";

        when(paymentRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(existingPayment);

        Payment firstCall = paymentService.createPayment(testUser, idempotencyKey, new BigDecimal("100.00"));
        Payment secondCall = paymentService.createPayment(testUser, idempotencyKey, new BigDecimal("100.00"));

        assertNotNull(firstCall);
        assertNotNull(secondCall);
        assertEquals(existingPayment.getId(), secondCall.getId());
        assertEquals(existingPayment.getIdempotencyKey(), secondCall.getIdempotencyKey());

        verify(paymentRepository, times(2)).findByIdempotencyKey(idempotencyKey);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void idempotencyKey_DifferentKeys_CreatesDifferentPayments() {
        String key1 = "unique-key-1";
        String key2 = "unique-key-2";

        Payment payment1 = new Payment();
        payment1.setId(1L);
        payment1.setUser(testUser);
        payment1.setIdempotencyKey(key1);
        payment1.setAmount(new BigDecimal("100.00"));
        payment1.setCurrency("INR");
        payment1.setStatus(PaymentStatus.PENDING);

        Payment payment2 = new Payment();
        payment2.setId(2L);
        payment2.setUser(testUser);
        payment2.setIdempotencyKey(key2);
        payment2.setAmount(new BigDecimal("200.00"));
        payment2.setCurrency("INR");
        payment2.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findByIdempotencyKey(key1)).thenReturn(Optional.empty());
        when(paymentRepository.findByIdempotencyKey(key2)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(payment1)
                .thenReturn(payment2);

        Payment result1 = paymentService.createPayment(testUser, key1, new BigDecimal("100.00"));
        Payment result2 = paymentService.createPayment(testUser, key2, new BigDecimal("200.00"));

        assertNotEquals(result1.getId(), result2.getId());
        assertNotEquals(result1.getIdempotencyKey(), result2.getIdempotencyKey());
        assertEquals(new BigDecimal("100.00"), result1.getAmount());
        assertEquals(new BigDecimal("200.00"), result2.getAmount());

        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    @Test
    void idempotencyKey_ConcurrentRequests_RaceConditionHandling() {
        String idempotencyKey = "concurrent-key-123";

        when(paymentRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingPayment));
        when(paymentRepository.save(any(Payment.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key violation"));

        Payment result = paymentService.createPayment(testUser, idempotencyKey, new BigDecimal("100.00"));

        assertNotNull(result);
        assertEquals(existingPayment.getId(), result.getId());
        assertEquals(existingPayment.getIdempotencyKey(), result.getIdempotencyKey());

        verify(paymentRepository, times(2)).findByIdempotencyKey(idempotencyKey);
    }

    @Test
    void idempotencyKey_ExistingPayment_ReturnsExistingWithOriginalData() {
        String idempotencyKey = "existing-key-123";

        existingPayment.setAmount(new BigDecimal("500.00"));
        existingPayment.setStatus(PaymentStatus.SUCCESS);

        when(paymentRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingPayment));

        Payment result = paymentService.createPayment(testUser, idempotencyKey, new BigDecimal("1000.00"));

        assertNotNull(result);
        assertEquals(existingPayment.getId(), result.getId());
        assertEquals(new BigDecimal("500.00"), result.getAmount());
        assertEquals(PaymentStatus.SUCCESS, result.getStatus());

        verify(paymentRepository, never()).save(any(Payment.class));
    }

}
