package com.codeorbitdemo.payment;
import com.codeorbitdemo.billing.Invoice;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity
@Table(name = "payments", indexes = @Index(name = "idx_payment_invoice", columnList = "invoice_id"))
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Invoice invoice;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentStatus status;
    @Column(nullable = false) private Instant createdAt;
    protected Payment() { }
    public Payment(Invoice invoice, BigDecimal amount) { this.invoice = invoice; this.amount = amount; this.status = PaymentStatus.SUCCEEDED; this.createdAt = Instant.now(); }
    public Long getId() { return id; } public Invoice getInvoice() { return invoice; } public BigDecimal getAmount() { return amount; } public PaymentStatus getStatus() { return status; } public Instant getCreatedAt() { return createdAt; }
}
