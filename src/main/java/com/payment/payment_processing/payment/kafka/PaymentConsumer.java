package com.payment.payment_processing.payment.kafka;

import com.payment.payment_processing.payment.entity.Payment;
import com.payment.payment_processing.payment.entity.PaymentStatus;
import com.payment.payment_processing.payment.event.PaymentEvent;
import com.payment.payment_processing.payment.repository.PaymentRepository;
import com.payment.payment_processing.payment.service.PaymentProcessor;
import com.payment.payment_processing.payment.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class PaymentConsumer {

    private final PaymentRepository paymentRepository;
    private final PaymentProcessor paymentProcessor;
    private final PaymentService paymentService;

    public PaymentConsumer(
            PaymentRepository paymentRepository,
            PaymentProcessor paymentProcessor,
            PaymentService paymentService) {

        this.paymentRepository = paymentRepository;
        this.paymentProcessor = paymentProcessor;
        this.paymentService = paymentService;
    }

    @KafkaListener(
            topics = "payment-created",
            groupId = "payment-processing-group"
    )
    public void consumePaymentCreatedEvent(PaymentEvent event) {

        System.out.println(
                "Payment event received: " +
                        "paymentId=" + event.getPaymentId() +
                        ", userId=" + event.getUserId() +
                        ", amount=" + event.getAmount() +
                        ", currency=" + event.getCurrency()
        );

        Payment payment = paymentRepository
                .findById(event.getPaymentId())
                .orElseThrow(() ->
                        new RuntimeException("Payment not found")
                );

        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);
        paymentService.clearPaymentCache(payment.getId());

        System.out.println(
                "Payment " + payment.getId() +
                        " is now PROCESSING"
        );

        paymentProcessor.processPayment(payment.getId());
    }
}