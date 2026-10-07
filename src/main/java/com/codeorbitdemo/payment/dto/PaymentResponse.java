package com.codeorbitdemo.payment.dto;
import com.codeorbitdemo.payment.Payment;
import com.codeorbitdemo.payment.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
public record PaymentResponse(Long id, Long invoiceId, BigDecimal amount, PaymentStatus status, Instant createdAt) {
    public static PaymentResponse from(Payment payment) { return new PaymentResponse(payment.getId(), payment.getInvoice().getId(), payment.getAmount(), payment.getStatus(), payment.getCreatedAt()); }
}
