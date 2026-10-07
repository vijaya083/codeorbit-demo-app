package com.codeorbitdemo.billing;
import com.codeorbitdemo.subscription.Subscription;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity
@Table(name = "invoices", indexes = @Index(name = "idx_invoice_subscription", columnList = "subscription_id"))
public class Invoice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Subscription subscription;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private InvoiceStatus status;
    @Column(nullable = false) private Instant issuedAt;
    @Column(nullable = false) private Instant dueAt;
    protected Invoice() { }
    public Invoice(Subscription subscription, BigDecimal amount, Instant issuedAt, Instant dueAt) { this.subscription = subscription; this.amount = amount; this.issuedAt = issuedAt; this.dueAt = dueAt; this.status = InvoiceStatus.OPEN; }
    public void markPaid() { this.status = InvoiceStatus.PAID; }
    public Long getId() { return id; } public Subscription getSubscription() { return subscription; } public BigDecimal getAmount() { return amount; } public InvoiceStatus getStatus() { return status; } public Instant getIssuedAt() { return issuedAt; } public Instant getDueAt() { return dueAt; }
}
