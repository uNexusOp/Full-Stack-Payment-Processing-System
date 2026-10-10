package com.payment.payment_processing.payment.controller;

import com.payment.payment_processing.payment.dto.CreatePaymentRequest;
import com.payment.payment_processing.payment.dto.PaymentResponse;
import com.payment.payment_processing.payment.entity.Payment;
import com.payment.payment_processing.payment.entity.PaymentStatus;
import com.payment.payment_processing.payment.service.PaymentService;
import com.payment.payment_processing.user.entity.User;
import com.payment.payment_processing.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PaymentController paymentController;

    private User testUser;
    private Payment testPayment;
    private PaymentResponse testResponse;

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

        testResponse = new PaymentResponse(
                1L,
                new BigDecimal("100.00"),
                "INR",
                PaymentStatus.PENDING,
                LocalDateTime.now()
        );
    }

    @Test
    void createPayment_Success() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setAmount(new BigDecimal("100.00"));

        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(paymentService.createPayment(any(User.class), anyString(), any(BigDecimal.class)))
                .thenReturn(testPayment);

        ResponseEntity<PaymentResponse> response = paymentController.createPayment(
                "test-key-123",
                request,
                authentication
        );

        assertNotNull(response);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());

        verify(userRepository).findByEmail("test@example.com");
        verify(paymentService).createPayment(any(User.class), anyString(), any(BigDecimal.class));
    }

    @Test
    void createPayment_UserNotFound_ThrowsException() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setAmount(new BigDecimal("100.00"));

        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            paymentController.createPayment("test-key-123", request, authentication);
        });

        verify(userRepository).findByEmail("test@example.com");
        verify(paymentService, never()).createPayment(any(User.class), anyString(), any(BigDecimal.class));
    }

    @Test
    void getPaymentById_Success() {
        when(paymentService.getPaymentById(1L)).thenReturn(testResponse);

        ResponseEntity<PaymentResponse> response = paymentController.getPaymentById(1L);

        assertNotNull(response);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());

        verify(paymentService).getPaymentById(1L);
    }

    @Test
    void getPaymentById_NotFound_ThrowsException() {
        when(paymentService.getPaymentById(999L))
                .thenThrow(new RuntimeException("Payment not found"));

        assertThrows(RuntimeException.class, () -> {
            paymentController.getPaymentById(999L);
        });

        verify(paymentService).getPaymentById(999L);
    }
}
