package com.codeorbitdemo.subscription.dto;
import com.codeorbitdemo.subscription.Subscription;
import com.codeorbitdemo.subscription.SubscriptionStatus;
import java.time.Instant;
public record SubscriptionResponse(Long id, Long userId, Long planId, String planCode, SubscriptionStatus status, Instant startedAt, Instant currentPeriodStart, Instant currentPeriodEnd, Instant cancelledAt) {
    public static SubscriptionResponse from(Subscription s) { return new SubscriptionResponse(s.getId(), s.getUser().getId(), s.getPlan().getId(), s.getPlan().getCode(), s.getStatus(), s.getStartedAt(), s.getCurrentPeriodStart(), s.getCurrentPeriodEnd(), s.getCancelledAt()); }
}
