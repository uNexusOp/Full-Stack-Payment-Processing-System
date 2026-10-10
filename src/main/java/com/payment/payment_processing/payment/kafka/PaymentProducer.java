package com.payment.payment_processing.payment.kafka;

import com.payment.payment_processing.payment.event.PaymentEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentProducer {

    private static final String TOPIC = "payment-created";

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public PaymentProducer(
            KafkaTemplate<String, PaymentEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendPaymentCreatedEvent(PaymentEvent event) {

        kafkaTemplate.send(
                TOPIC,
                event.getPaymentId().toString(),
                event
        );
    }
}