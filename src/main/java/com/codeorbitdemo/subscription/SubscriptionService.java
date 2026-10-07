package com.codeorbitdemo.subscription;

import com.codeorbitdemo.billing.BillingService;
import com.codeorbitdemo.common.exception.ApiException;
import com.codeorbitdemo.notification.NotificationService;
import com.codeorbitdemo.subscription.dto.SubscriptionResponse;
import com.codeorbitdemo.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class SubscriptionService {
    private final SubscriptionRepository subscriptions; private final UserService users; private final PlanService plans; private final BillingService billing; private final NotificationService notifications;
    public SubscriptionService(SubscriptionRepository subscriptions, UserService users, PlanService plans, BillingService billing, NotificationService notifications) { this.subscriptions = subscriptions; this.users = users; this.plans = plans; this.billing = billing; this.notifications = notifications; }
    @Transactional public SubscriptionResponse create(Long userId, Long planId) {
        var user = users.requireUser(userId); var plan = plans.requireActive(planId);
        if (subscriptions.existsByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)) throw new ApiException(HttpStatus.CONFLICT, "User already has an active subscription");
        Instant now = Instant.now(); Subscription saved = subscriptions.save(new Subscription(user, plan, now, now.plus(30, ChronoUnit.DAYS)));
        billing.createInitialInvoice(saved); notifications.subscriptionCreated(saved);
        return SubscriptionResponse.from(saved);
    }
    @Transactional public SubscriptionResponse cancel(Long id) {
        Subscription subscription = subscriptions.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subscription not found"));
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) throw new ApiException(HttpStatus.CONFLICT, "Subscription is not active");
        subscription.cancel(Instant.now()); return SubscriptionResponse.from(subscriptions.save(subscription));
    }
    @Transactional(readOnly = true) public List<SubscriptionResponse> forUser(Long userId) {
        users.requireUser(userId); return subscriptions.findAllByUserIdOrderByStartedAtDesc(userId).stream().map(SubscriptionResponse::from).toList();
    }
}
