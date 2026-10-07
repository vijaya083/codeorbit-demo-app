package com.codeorbitdemo.refund;
import com.codeorbitdemo.payment.Payment;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity
@Table(name = "refunds", indexes = @Index(name = "idx_refund_payment", columnList = "payment_id"))
public class Refund {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Payment payment;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private RefundStatus status;
    @Column(nullable = false) private Instant createdAt;
    protected Refund() { }
    public Refund(Payment payment, BigDecimal amount) { this.payment = payment; this.amount = amount; this.status = RefundStatus.COMPLETED; this.createdAt = Instant.now(); }
    public Long getId() { return id; } public Payment getPayment() { return payment; } public BigDecimal getAmount() { return amount; } public RefundStatus getStatus() { return status; } public Instant getCreatedAt() { return createdAt; }
}
