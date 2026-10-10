package com.payment.payment_processing.payment.service;

import com.payment.payment_processing.payment.dto.PaymentResponse;
import com.payment.payment_processing.payment.entity.Payment;
import com.payment.payment_processing.payment.entity.PaymentStatus;
import com.payment.payment_processing.payment.event.PaymentEvent;
import com.payment.payment_processing.payment.kafka.PaymentProducer;
import com.payment.payment_processing.payment.repository.PaymentRepository;
import com.payment.payment_processing.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
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
class PaymentServiceTest {

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

    private User testUser;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");

        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setUser(testUser);
        testPayment.setIdempotencyKey("test-key-123");
        testPayment.setAmount(new BigDecimal("100.00"));
        testPayment.setCurrency("INR");
        testPayment.setStatus(PaymentStatus.PENDING);
        testPayment.setCreatedAt(LocalDateTime.now());
        testPayment.setUpdatedAt(LocalDateTime.now());

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void createPayment_Success() {
        when(paymentRepository.findByIdempotencyKey("test-key-123")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        doNothing().when(paymentProducer).sendPaymentCreatedEvent(any(PaymentEvent.class));

        Payment result = paymentService.createPayment(testUser, "test-key-123", new BigDecimal("100.00"));

        assertNotNull(result);
        assertEquals("test-key-123", result.getIdempotencyKey());
        assertEquals(new BigDecimal("100.00"), result.getAmount());
        assertEquals(PaymentStatus.PENDING, result.getStatus());

        verify(paymentRepository).save(any(Payment.class));
        verify(paymentProducer).sendPaymentCreatedEvent(any(PaymentEvent.class));
    }

    @Test
    void createPayment_IdempotencyKeyExists_ReturnsExistingPayment() {
        when(paymentRepository.findByIdempotencyKey("test-key-123")).thenReturn(Optional.of(testPayment));

        Payment result = paymentService.createPayment(testUser, "test-key-123", new BigDecimal("100.00"));

        assertNotNull(result);
        assertEquals(testPayment.getId(), result.getId());
        assertEquals(testPayment.getIdempotencyKey(), result.getIdempotencyKey());

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(paymentProducer, never()).sendPaymentCreatedEvent(any(PaymentEvent.class));
    }

    @Test
    void createPayment_DataIntegrityViolationException_RetriesAndReturnsExisting() {
        when(paymentRepository.findByIdempotencyKey("test-key-123"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenThrow(new DataIntegrityViolationException("Duplicate key"));

        Payment result = paymentService.createPayment(testUser, "test-key-123", new BigDecimal("100.00"));

        assertNotNull(result);
        assertEquals(testPayment.getId(), result.getId());

        verify(paymentRepository, times(2)).findByIdempotencyKey("test-key-123");
    }

    @Test
    void createPayment_DataIntegrityViolationException_FailsToFindExisting_ThrowsException() {
        when(paymentRepository.findByIdempotencyKey("test-key-123"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenThrow(new DataIntegrityViolationException("Duplicate key"));

        assertThrows(RuntimeException.class, () -> {
            paymentService.createPayment(testUser, "test-key-123", new BigDecimal("100.00"));
        });
    }

    @Test
    void getPaymentById_CacheHit_ReturnsCachedPayment() {
        PaymentResponse cachedResponse = new PaymentResponse(
                1L,
                new BigDecimal("100.00"),
                "INR",
                PaymentStatus.PENDING,
                LocalDateTime.now()
        );

        when(valueOperations.get("payment:1")).thenReturn(cachedResponse);

        PaymentResponse result = paymentService.getPaymentById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(new BigDecimal("100.00"), result.getAmount());

        verify(paymentRepository, never()).findById(anyLong());
        verify(valueOperations, never()).set(anyString(), any(PaymentResponse.class));
    }

    @Test
    void getPaymentById_CacheMiss_FetchesFromDatabaseAndCaches() {
        when(valueOperations.get("payment:1")).thenReturn(null);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

        PaymentResponse result = paymentService.getPaymentById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(new BigDecimal("100.00"), result.getAmount());

        verify(paymentRepository).findById(1L);
        verify(valueOperations).set(eq("payment:1"), any(PaymentResponse.class));
    }

    @Test
    void getPaymentById_PaymentNotFound_ThrowsException() {
        when(valueOperations.get("payment:1")).thenReturn(null);
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            paymentService.getPaymentById(1L);
        });
    }

    @Test
    void clearPaymentCache_Success() {
        when(redisTemplate.delete("payment:1")).thenReturn(true);

        paymentService.clearPaymentCache(1L);

        verify(redisTemplate).delete("payment:1");
    }
}
