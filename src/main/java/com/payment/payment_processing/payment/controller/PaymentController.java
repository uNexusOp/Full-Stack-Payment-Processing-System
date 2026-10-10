package com.payment.payment_processing.payment.controller;

import com.payment.payment_processing.payment.dto.CreatePaymentRequest;
import com.payment.payment_processing.payment.dto.PaymentResponse;
import com.payment.payment_processing.payment.entity.Payment;
import com.payment.payment_processing.payment.service.PaymentService;
import com.payment.payment_processing.user.entity.User;
import com.payment.payment_processing.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    public PaymentController(
            PaymentService paymentService,
            UserRepository userRepository) {
        this.paymentService = paymentService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request,
            Authentication authentication) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Payment payment = paymentService.createPayment(
                user,
                idempotencyKey,
                request.getAmount()
        );

        PaymentResponse response = new PaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getCreatedAt()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(
            @PathVariable Long id) {

        PaymentResponse response = paymentService.getPaymentById(id);

        return ResponseEntity.ok(response);
    }
}