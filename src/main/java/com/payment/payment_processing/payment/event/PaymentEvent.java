package com.payment.payment_processing.payment.event;

import java.math.BigDecimal;

public class PaymentEvent {

    private Long paymentId;
    private Long userId;
    private BigDecimal amount;
    private String currency;

    public PaymentEvent() {
    }

    public PaymentEvent(
            Long paymentId,
            Long userId,
            BigDecimal amount,
            String currency) {

        this.paymentId = paymentId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}