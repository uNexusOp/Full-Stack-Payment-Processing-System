package com.payment.payment_processing.payment.service;

import com.payment.payment_processing.payment.entity.Payment;
import com.payment.payment_processing.payment.entity.PaymentStatus;
import com.payment.payment_processing.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import com.payment.payment_processing.payment.service.PaymentService;

@Service
public class PaymentProcessor {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    public PaymentProcessor(
            PaymentRepository paymentRepository,
            PaymentService paymentService) {

        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
    }

    public void processPayment(Long paymentId) {

        Payment payment = paymentRepository
                .findById(paymentId)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found")
                );

        System.out.println(
                "Processing payment: " + payment.getId()
        );

        // Simulate payment gateway processing
        boolean paymentSuccessful = payment.getAmount().compareTo(
                new java.math.BigDecimal("10000")
        ) <= 0;

        if (paymentSuccessful) {
            payment.setStatus(PaymentStatus.SUCCESS);

            System.out.println(
                    "Payment " + payment.getId() +
                            " completed successfully"
            );
        } else {
            payment.setStatus(PaymentStatus.FAILED);

            System.out.println(
                    "Payment " + payment.getId() +
                            " failed"
            );
        }

        paymentRepository.save(payment);
        paymentService.clearPaymentCache(payment.getId());
    }
}