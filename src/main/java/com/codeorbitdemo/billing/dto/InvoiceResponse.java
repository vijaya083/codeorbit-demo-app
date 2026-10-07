package com.codeorbitdemo.billing.dto;
import com.codeorbitdemo.billing.Invoice;
import com.codeorbitdemo.billing.InvoiceStatus;
import java.math.BigDecimal;
import java.time.Instant;
public record InvoiceResponse(Long id, Long subscriptionId, BigDecimal amount, InvoiceStatus status, Instant issuedAt, Instant dueAt) {
    public static InvoiceResponse from(Invoice invoice) { return new InvoiceResponse(invoice.getId(), invoice.getSubscription().getId(), invoice.getAmount(), invoice.getStatus(), invoice.getIssuedAt(), invoice.getDueAt()); }
}
