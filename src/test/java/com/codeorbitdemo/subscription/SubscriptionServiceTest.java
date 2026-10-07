package com.codeorbitdemo.subscription;

import com.codeorbitdemo.billing.BillingService;
import com.codeorbitdemo.common.exception.ApiException;
import com.codeorbitdemo.notification.NotificationService;
import com.codeorbitdemo.user.User;
import com.codeorbitdemo.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {
    @Mock SubscriptionRepository subscriptions;
    @Mock UserService users;
    @Mock PlanService plans;
    @Mock BillingService billing;
    @Mock NotificationService notifications;

    @Test
    void createsSubscriptionForExistingUserAndActivePlan() {
        when(users.requireUser(1L)).thenReturn(user());
        when(plans.requireActive(2L)).thenReturn(plan());
        when(subscriptions.existsByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE)).thenReturn(false);
        when(subscriptions.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service().create(1L, 2L);

        assertEquals(SubscriptionStatus.ACTIVE, response.status());
        assertEquals("BASIC", response.planCode());
        verify(subscriptions).save(any(Subscription.class));
        verify(notifications).subscriptionCreated(any(Subscription.class));
    }

    @Test
    void createsInitialInvoiceAsPartOfSubscriptionCreation() {
        when(users.requireUser(1L)).thenReturn(user());
        when(plans.requireActive(2L)).thenReturn(plan());
        when(subscriptions.existsByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE)).thenReturn(false);
        when(subscriptions.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service().create(1L, 2L);

        ArgumentCaptor<Subscription> created = ArgumentCaptor.forClass(Subscription.class);
        verify(billing).createInitialInvoice(created.capture());
        assertEquals(SubscriptionStatus.ACTIVE, created.getValue().getStatus());
    }

    @Test
    void rejectsAnotherActiveSubscription() {
        when(users.requireUser(1L)).thenReturn(user());
        when(plans.requireActive(2L)).thenReturn(plan());
        when(subscriptions.existsByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE)).thenReturn(true);

        var error = assertThrows(ApiException.class, () -> service().create(1L, 2L));

        assertEquals(HttpStatus.CONFLICT, error.status());
        verify(subscriptions, never()).save(any());
        verifyNoInteractions(billing, notifications);
    }

    @Test
    void cancelsAnActiveSubscriptionAndRecordsTimestamp() {
        Subscription active = subscription();
        when(subscriptions.findById(3L)).thenReturn(Optional.of(active));
        when(subscriptions.save(active)).thenReturn(active);

        var result = service().cancel(3L);

        assertEquals(SubscriptionStatus.CANCELLED, result.status());
        assertNotNull(result.cancelledAt());
        verify(subscriptions).save(active);
    }

    @Test
    void rejectsCancellationWhenSubscriptionIsAlreadyCancelled() {
        Subscription cancelled = subscription();
        cancelled.cancel(Instant.now());
        when(subscriptions.findById(3L)).thenReturn(Optional.of(cancelled));

        var error = assertThrows(ApiException.class, () -> service().cancel(3L));

        assertEquals(HttpStatus.CONFLICT, error.status());
        verify(subscriptions, never()).save(any());
    }

    private SubscriptionService service() { return new SubscriptionService(subscriptions, users, plans, billing, notifications); }
    private static User user() { return new User("ada@example.test", "hash", "Ada", "Lovelace"); }
    private static Plan plan() { return new Plan("BASIC", "Basic", new BigDecimal("9.00"), true); }
    private static Subscription subscription() { Instant now = Instant.now(); return new Subscription(user(), plan(), now, now.plusSeconds(86400)); }
}
