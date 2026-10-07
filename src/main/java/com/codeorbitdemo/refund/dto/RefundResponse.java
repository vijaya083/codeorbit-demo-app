package com.codeorbitdemo.refund.dto;
import com.codeorbitdemo.refund.Refund;
import com.codeorbitdemo.refund.RefundStatus;
import java.math.BigDecimal;
import java.time.Instant;
public record RefundResponse(Long id, Long paymentId, BigDecimal amount, RefundStatus status, Instant createdAt) {
    public static RefundResponse from(Refund refund) { return new RefundResponse(refund.getId(), refund.getPayment().getId(), refund.getAmount(), refund.getStatus(), refund.getCreatedAt()); }
}
