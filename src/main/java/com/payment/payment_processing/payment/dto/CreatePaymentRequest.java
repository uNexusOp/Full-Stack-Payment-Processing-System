package com.payment.payment_processing.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class CreatePaymentRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(
            value = "1.00",
            message = "Amount must be at least 1.00"
    )
    private BigDecimal amount;

    public CreatePaymentRequest() {
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}