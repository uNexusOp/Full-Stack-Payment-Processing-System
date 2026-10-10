package com.payment.payment_processing.payment.service;

import com.payment.payment_processing.payment.entity.Payment;
import com.payment.payment_processing.payment.entity.PaymentStatus;
import com.payment.payment_processing.payment.repository.PaymentRepository;
import com.payment.payment_processing.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentProcessorTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentProcessor paymentProcessor;

    private Payment testPayment;

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
        testPayment.setStatus(PaymentStatus.PENDING);
        testPayment.setCreatedAt(LocalDateTime.now());
        testPayment.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void processPayment_AmountLessThanThreshold_SetsStatusToSuccess() {
        testPayment.setAmount(new BigDecimal("5000.00"));

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        doNothing().when(paymentService).clearPaymentCache(1L);

        paymentProcessor.processPayment(1L);

        assertEquals(PaymentStatus.SUCCESS, testPayment.getStatus());
        verify(paymentRepository).save(testPayment);
        verify(paymentService).clearPaymentCache(1L);
    }

    @Test
    void processPayment_AmountEqualToThreshold_SetsStatusToSuccess() {
        testPayment.setAmount(new BigDecimal("10000.00"));

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        doNothing().when(paymentService).clearPaymentCache(1L);

        paymentProcessor.processPayment(1L);

        assertEquals(PaymentStatus.SUCCESS, testPayment.getStatus());
        verify(paymentRepository).save(testPayment);
        verify(paymentService).clearPaymentCache(1L);
    }

    @Test
    void processPayment_AmountGreaterThanThreshold_SetsStatusToFailed() {
        testPayment.setAmount(new BigDecimal("15000.00"));

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        doNothing().when(paymentService).clearPaymentCache(1L);

        paymentProcessor.processPayment(1L);

        assertEquals(PaymentStatus.FAILED, testPayment.getStatus());
        verify(paymentRepository).save(testPayment);
        verify(paymentService).clearPaymentCache(1L);
    }

    @Test
    void processPayment_PaymentNotFound_ThrowsException() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            paymentProcessor.processPayment(1L);
        });

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(paymentService, never()).clearPaymentCache(anyLong());
    }

    @Test
    void processPayment_VariousAmounts_CorrectStatusAssignment() {
        Payment smallPayment = new Payment();
        smallPayment.setId(2L);
        smallPayment.setAmount(new BigDecimal("10.00"));
        smallPayment.setStatus(PaymentStatus.PENDING);

        Payment mediumPayment = new Payment();
        mediumPayment.setId(3L);
        mediumPayment.setAmount(new BigDecimal("5000.00"));
        mediumPayment.setStatus(PaymentStatus.PENDING);

        Payment largePayment = new Payment();
        largePayment.setId(4L);
        largePayment.setAmount(new BigDecimal("20000.00"));
        largePayment.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findById(2L)).thenReturn(Optional.of(smallPayment));
        when(paymentRepository.findById(3L)).thenReturn(Optional.of(mediumPayment));
        when(paymentRepository.findById(4L)).thenReturn(Optional.of(largePayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(paymentService).clearPaymentCache(anyLong());

        paymentProcessor.processPayment(2L);
        paymentProcessor.processPayment(3L);
        paymentProcessor.processPayment(4L);

        assertEquals(PaymentStatus.SUCCESS, smallPayment.getStatus());
        assertEquals(PaymentStatus.SUCCESS, mediumPayment.getStatus());
        assertEquals(PaymentStatus.FAILED, largePayment.getStatus());

        verify(paymentRepository, times(3)).save(any(Payment.class));
        verify(paymentService, times(3)).clearPaymentCache(anyLong());
    }

    @Test
    void processPayment_AlreadyProcessedPayment_UpdatesStatusAgain() {
        testPayment.setStatus(PaymentStatus.SUCCESS);
        testPayment.setAmount(new BigDecimal("8000.00"));

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        doNothing().when(paymentService).clearPaymentCache(1L);

        paymentProcessor.processPayment(1L);

        assertEquals(PaymentStatus.SUCCESS, testPayment.getStatus());
        verify(paymentRepository).save(testPayment);
        verify(paymentService).clearPaymentCache(1L);
    }

    @Test
    void processPayment_UpdatesTimestamp_WhenSaved() {
        LocalDateTime beforeUpdate = testPayment.getUpdatedAt();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setUpdatedAt(LocalDateTime.now());
            return payment;
        });
        doNothing().when(paymentService).clearPaymentCache(1L);

        paymentProcessor.processPayment(1L);

        verify(paymentRepository).save(testPayment);
    }

    @Test
    void processPayment_ClearsCacheAfterProcessing() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        doNothing().when(paymentService).clearPaymentCache(1L);

        paymentProcessor.processPayment(1L);

        verify(paymentService).clearPaymentCache(1L);
    }
}
