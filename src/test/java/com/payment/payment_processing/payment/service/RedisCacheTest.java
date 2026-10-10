package com.payment.payment_processing.payment.service;

import com.payment.payment_processing.payment.dto.PaymentResponse;
import com.payment.payment_processing.payment.entity.Payment;
import com.payment.payment_processing.payment.entity.PaymentStatus;
import com.payment.payment_processing.payment.kafka.PaymentProducer;
import com.payment.payment_processing.payment.repository.PaymentRepository;
import com.payment.payment_processing.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisCacheTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentProducer paymentProducer;

    @Mock
    private RedisTemplate<String, PaymentResponse> redisTemplate;

    @Mock
    private ValueOperations<String, PaymentResponse> valueOperations;

    @InjectMocks
    private PaymentService paymentService;

    private Payment testPayment;
    private PaymentResponse cachedResponse;

    @BeforeEach
    void setUp() {
        User testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setUser(testUser);
        testPayment.setIdempotencyKey("test-key-123");
        testPayment.setAmount(new BigDecimal("100.00"));
        testPayment.setCurrency("INR");
        testPayment.setStatus(PaymentStatus.SUCCESS);
        testPayment.setCreatedAt(LocalDateTime.now());
        testPayment.setUpdatedAt(LocalDateTime.now());

        cachedResponse = new PaymentResponse(
                1L,
                new BigDecimal("100.00"),
                "INR",
                PaymentStatus.SUCCESS,
                LocalDateTime.now()
        );

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void redisCache_Hit_ReturnsCachedResponseWithoutDatabaseCall() {
        when(valueOperations.get("payment:1")).thenReturn(cachedResponse);

        PaymentResponse result = paymentService.getPaymentById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(new BigDecimal("100.00"), result.getAmount());
        assertEquals(PaymentStatus.SUCCESS, result.getStatus());

        verify(redisTemplate.opsForValue()).get("payment:1");
        verify(paymentRepository, never()).findById(anyLong());
        verify(valueOperations, never()).set(anyString(), any(PaymentResponse.class));
    }

    @Test
    void redisCache_Miss_FetchesFromDatabaseAndCaches() {
        when(valueOperations.get("payment:1")).thenReturn(null);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

        PaymentResponse result = paymentService.getPaymentById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(new BigDecimal("100.00"), result.getAmount());

        verify(redisTemplate.opsForValue()).get("payment:1");
        verify(paymentRepository).findById(1L);
        verify(valueOperations).set(eq("payment:1"), any(PaymentResponse.class));
    }

    @Test
    void redisCache_SequentialCalls_FirstMissThenHit() {
        when(valueOperations.get("payment:1"))
                .thenReturn(null)
                .thenReturn(cachedResponse);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

        PaymentResponse firstCall = paymentService.getPaymentById(1L);
        PaymentResponse secondCall = paymentService.getPaymentById(1L);

        assertNotNull(firstCall);
        assertNotNull(secondCall);
        assertEquals(firstCall.getId(), secondCall.getId());

        verify(paymentRepository, times(1)).findById(1L);
        verify(valueOperations, times(1)).set(eq("payment:1"), any(PaymentResponse.class));
    }

    @Test
    void redisCache_ClearCache_RemovesCachedData() {
        when(redisTemplate.delete("payment:1")).thenReturn(true);

        paymentService.clearPaymentCache(1L);

        verify(redisTemplate).delete("payment:1");
    }

    @Test
    void redisCache_ClearCache_NonExistentKey_ReturnsFalse() {
        when(redisTemplate.delete("payment:999")).thenReturn(false);

        paymentService.clearPaymentCache(999L);

        verify(redisTemplate).delete("payment:999");
    }

    @Test
    void redisCache_DifferentPaymentIds_UseDifferentCacheKeys() {
        Payment payment2 = new Payment();
        payment2.setId(2L);
        payment2.setAmount(new BigDecimal("200.00"));
        payment2.setCurrency("INR");
        payment2.setStatus(PaymentStatus.PENDING);
        payment2.setCreatedAt(LocalDateTime.now());

        PaymentResponse cachedResponse2 = new PaymentResponse(
                2L,
                new BigDecimal("200.00"),
                "INR",
                PaymentStatus.PENDING,
                LocalDateTime.now()
        );

        when(valueOperations.get("payment:1")).thenReturn(cachedResponse);
        when(valueOperations.get("payment:2")).thenReturn(cachedResponse2);

        PaymentResponse result1 = paymentService.getPaymentById(1L);
        PaymentResponse result2 = paymentService.getPaymentById(2L);

        assertEquals(1L, result1.getId());
        assertEquals(2L, result2.getId());

        verify(valueOperations).get("payment:1");
        verify(valueOperations).get("payment:2");
    }

    @Test
    void redisCache_CacheInvalidation_ClearsAfterPaymentUpdate() {
        when(valueOperations.get("payment:1")).thenReturn(cachedResponse);
        when(redisTemplate.delete("payment:1")).thenReturn(true);

        paymentService.getPaymentById(1L);
        paymentService.clearPaymentCache(1L);

        verify(valueOperations).get("payment:1");
        verify(redisTemplate).delete("payment:1");
    }

    @Test
    void redisCache_MissWithDatabaseError_ThrowsException() {
        when(valueOperations.get("payment:1")).thenReturn(null);
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            paymentService.getPaymentById(1L);
        });

        verify(paymentRepository).findById(1L);
        verify(valueOperations, never()).set(anyString(), any(PaymentResponse.class));
    }
}
