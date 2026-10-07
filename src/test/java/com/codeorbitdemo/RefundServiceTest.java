package com.codeorbitdemo;

import com.codeorbitdemo.common.exception.ApiException;
import com.codeorbitdemo.payment.Payment;
import com.codeorbitdemo.payment.PaymentService;
import com.codeorbitdemo.refund.Refund;
import com.codeorbitdemo.refund.RefundRepository;
import com.codeorbitdemo.refund.RefundService;
import com.codeorbitdemo.subscription.Plan;
import com.codeorbitdemo.subscription.Subscription;
import com.codeorbitdemo.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {
    @Mock PaymentService payments;
    @Mock RefundRepository refunds;

    @Test
    void completesRefundAgainstSuccessfulPayment() {
        Payment payment = payment("100.00");
        when(payments.requireSuccessful(42L)).thenReturn(payment);
        when(refunds.sumCompletedAmount(42L)).thenReturn(BigDecimal.ZERO);
        when(refunds.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service().refund(42L, new BigDecimal("25.00"));

        assertEquals(new BigDecimal("25.00"), result.amount());
        assertEquals(com.codeorbitdemo.refund.RefundStatus.COMPLETED, result.status());
        verify(refunds).save(any(Refund.class));
    }

    @Test
    void rejectsRefundGreaterThanRemainingRefundableAmount() {
        when(payments.requireSuccessful(42L)).thenReturn(payment("100.00"));
        when(refunds.sumCompletedAmount(42L)).thenReturn(new BigDecimal("90.00"));

        var error = assertThrows(ApiException.class, () -> service().refund(42L, new BigDecimal("15.00")));

        assertEquals(HttpStatus.BAD_REQUEST, error.status());
        verify(refunds, never()).save(any());
    }

    @Test
    void completedRefundsCannotCumulativelyExceedPaymentAmount() {
        Payment payment = payment("100.00");
        List<Refund> completed = new ArrayList<>();
        when(payments.requireSuccessful(42L)).thenReturn(payment);
        when(refunds.sumCompletedAmount(42L)).thenAnswer(invocation -> completed.stream()
                .map(Refund::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        when(refunds.save(any(Refund.class))).thenAnswer(invocation -> {
            Refund refund = invocation.getArgument(0);
            completed.add(refund);
            return refund;
        });
        RefundService service = service();

        service.refund(42L, new BigDecimal("60.00"));
        service.refund(42L, new BigDecimal("40.00"));
        var error = assertThrows(ApiException.class, () -> service.refund(42L, new BigDecimal("0.01")));

        assertEquals(HttpStatus.BAD_REQUEST, error.status());
        assertEquals(new BigDecimal("100.00"), completed.stream().map(Refund::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        verify(refunds, times(2)).save(any(Refund.class));
    }

    @Test
    void rejectsRefundWhenPaymentDidNotSucceed() {
        when(payments.requireSuccessful(42L)).thenThrow(new ApiException(HttpStatus.CONFLICT, "Payment did not succeed"));

        var error = assertThrows(ApiException.class, () -> service().refund(42L, new BigDecimal("5.00")));

        assertEquals(HttpStatus.CONFLICT, error.status());
        verifyNoInteractions(refunds);
    }

    private RefundService service() { return new RefundService(refunds, payments); }
    private static Payment payment(String amount) {
        Instant now = Instant.now();
        User user = new User("ada@example.test", "hash", "Ada", "Lovelace");
        Plan plan = new Plan("PRO", "Pro", new BigDecimal(amount), true);
        Subscription subscription = new Subscription(user, plan, now, now.plusSeconds(86400));
        var invoice = new com.codeorbitdemo.billing.Invoice(subscription, new BigDecimal(amount), now, now.plusSeconds(86400));
        return new Payment(invoice, new BigDecimal(amount));
    }
}
