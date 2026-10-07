package com.codeorbitdemo.subscription;
import com.codeorbitdemo.user.User;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name = "subscriptions", indexes = @Index(name = "idx_subscription_user_status", columnList = "user_id,status"))
public class Subscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Plan plan;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private SubscriptionStatus status;
    @Column(nullable = false) private Instant startedAt;
    @Column(nullable = false) private Instant currentPeriodStart;
    @Column(nullable = false) private Instant currentPeriodEnd;
    private Instant cancelledAt;
    protected Subscription() { }
    public Subscription(User user, Plan plan, Instant start, Instant end) { this.user = user; this.plan = plan; this.startedAt = start; this.currentPeriodStart = start; this.currentPeriodEnd = end; this.status = SubscriptionStatus.ACTIVE; }
    public void cancel(Instant at) { this.status = SubscriptionStatus.CANCELLED; this.cancelledAt = at; }
    public Long getId() { return id; } public User getUser() { return user; } public Plan getPlan() { return plan; } public SubscriptionStatus getStatus() { return status; } public Instant getStartedAt() { return startedAt; } public Instant getCurrentPeriodStart() { return currentPeriodStart; } public Instant getCurrentPeriodEnd() { return currentPeriodEnd; } public Instant getCancelledAt() { return cancelledAt; }
}
