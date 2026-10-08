package com.codeorbitdemo.payment;

import com.codeorbitdemo.billing.Invoice;
import com.codeorbitdemo.billing.InvoiceService;
import com.codeorbitdemo.common.exception.ApiException;
import com.codeorbitdemo.notification.NotificationService;
import com.codeorbitdemo.subscription.Plan;
import com.codeorbitdemo.subscription.Subscription;
import com.codeorbitdemo.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock PaymentRepository payments;
    @Mock InvoiceService invoices;
    @Mock NotificationService notifications;

    @Test
    void recordsSuccessfulPaymentAndMarksInvoicePaid() {
        Invoice invoice = invoice();
        when(invoices.requireInvoice(8L)).thenReturn(invoice);
        when(payments.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> { invoice.markPaid(); return null; }).when(invoices).markPaid(invoice);

        var response = new PaymentService(payments, invoices, notifications).recordPayment(8L, new BigDecimal("29.00"));

        assertEquals(PaymentStatus.SUCCEEDED, response.status());
        assertEquals(new BigDecimal("29.00"), response.amount());
        assertEquals(com.codeorbitdemo.billing.InvoiceStatus.PAID, invoice.getStatus());
        verify(invoices).markPaid(invoice);
        verifyNoInteractions(notifications);
    }

    @Test
    void recordsFailedPaymentAndNotifiesWithoutMarkingInvoicePaid() {
        Invoice invoice = invoice();
        when(invoices.requireInvoice(8L)).thenReturn(invoice);
        when(payments.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = new PaymentService(payments, invoices, notifications)
                .recordPayment(8L, new BigDecimal("29.00"), true);

        assertEquals(PaymentStatus.FAILED, response.status());
        assertEquals(com.codeorbitdemo.billing.InvoiceStatus.OPEN, invoice.getStatus());
        ArgumentCaptor<Payment> savedPayment = ArgumentCaptor.forClass(Payment.class);
        verify(payments).save(savedPayment.capture());
        assertEquals(PaymentStatus.FAILED, savedPayment.getValue().getStatus());
        assertSame(invoice, savedPayment.getValue().getInvoice());
        verify(invoices, never()).markPaid(any());
        verify(notifications).paymentFailed(invoice);
    }

    @Test
    void rejectsNonPositiveOrMismatchedInvoiceAmounts() {
        Invoice invoice = invoice();
        when(invoices.requireInvoice(8L)).thenReturn(invoice);
        PaymentService service = new PaymentService(payments, invoices, notifications);

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class,
                () -> service.recordPayment(8L, new BigDecimal("0.00"))).status());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class,
                () -> service.recordPayment(8L, new BigDecimal("28.99"))).status());
        verifyNoInteractions(payments);
        verify(invoices, never()).markPaid(any());
    }

    @Test
    void rejectsPaymentAgainstInvoiceThatIsNotOpen() {
        Invoice invoice = invoice();
        invoice.markPaid();
        when(invoices.requireInvoice(8L)).thenReturn(invoice);

        var error = assertThrows(ApiException.class,
                () -> new PaymentService(payments, invoices, notifications).recordPayment(8L, new BigDecimal("29.00")));

        assertEquals(HttpStatus.CONFLICT, error.status());
        verifyNoInteractions(payments);
        verify(invoices, never()).markPaid(any());
    }

    @Test
    void retryCreatesANewSuccessfulAttemptAndMarksTheInvoicePaid() {
        Invoice invoice = invoice();
        Payment original = new Payment(invoice, new BigDecimal("29.00"), PaymentStatus.FAILED);
        when(payments.findById(11L)).thenReturn(Optional.of(original));
        when(invoices.requireInvoice(any())).thenReturn(invoice);
        when(payments.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> { invoice.markPaid(); return null; }).when(invoices).markPaid(invoice);

        var response = new PaymentService(payments, invoices, notifications).retryPayment(11L, false);

        ArgumentCaptor<Payment> savedPayment = ArgumentCaptor.forClass(Payment.class);
        verify(payments).save(savedPayment.capture());
        Payment retry = savedPayment.getValue();
        assertNotSame(original, retry);
        assertEquals(PaymentStatus.FAILED, original.getStatus());
        assertEquals(PaymentStatus.SUCCEEDED, retry.getStatus());
        assertSame(invoice, retry.getInvoice());
        assertEquals(new BigDecimal("29.00"), retry.getAmount());
        assertEquals(PaymentStatus.SUCCEEDED, response.status());
        assertEquals(com.codeorbitdemo.billing.InvoiceStatus.PAID, invoice.getStatus());
        verify(invoices).markPaid(invoice);
        verifyNoInteractions(notifications);
    }

    @Test
    void failedRetryCreatesAnotherFailedAttemptAndLeavesInvoiceOpen() {
        Invoice invoice = invoice();
        Payment original = new Payment(invoice, new BigDecimal("29.00"), PaymentStatus.FAILED);
        when(payments.findById(11L)).thenReturn(Optional.of(original));
        when(invoices.requireInvoice(any())).thenReturn(invoice);
        when(payments.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = new PaymentService(payments, invoices, notifications).retryPayment(11L, true);

        ArgumentCaptor<Payment> savedPayment = ArgumentCaptor.forClass(Payment.class);
        verify(payments).save(savedPayment.capture());
        assertNotSame(original, savedPayment.getValue());
        assertEquals(PaymentStatus.FAILED, original.getStatus());
        assertEquals(PaymentStatus.FAILED, response.status());
        assertEquals(com.codeorbitdemo.billing.InvoiceStatus.OPEN, invoice.getStatus());
        verify(invoices, never()).markPaid(any());
        verify(notifications).paymentFailed(invoice);
    }

    @Test
    void rejectsRetryOfPaymentThatDidNotFail() {
        Payment payment = new Payment(invoice(), new BigDecimal("29.00"), PaymentStatus.SUCCEEDED);
        when(payments.findById(11L)).thenReturn(Optional.of(payment));

        var error = assertThrows(ApiException.class,
                () -> new PaymentService(payments, invoices, notifications).retryPayment(11L, false));

        assertEquals(HttpStatus.CONFLICT, error.status());
        verify(invoices, never()).requireInvoice(any());
        verify(payments, never()).save(any());
    }

    @Test
    void rejectsRetryWhenInvoiceIsNoLongerOpen() {
        Invoice invoice = invoice();
        invoice.markPaid();
        Payment failedPayment = new Payment(invoice, new BigDecimal("29.00"), PaymentStatus.FAILED);
        when(payments.findById(11L)).thenReturn(Optional.of(failedPayment));
        when(invoices.requireInvoice(any())).thenReturn(invoice);

        var error = assertThrows(ApiException.class,
                () -> new PaymentService(payments, invoices, notifications).retryPayment(11L, false));

        assertEquals(HttpStatus.CONFLICT, error.status());
        verify(payments, never()).save(any());
        verify(invoices, never()).markPaid(any());
    }

    private static Invoice invoice() {
        Instant now = Instant.now();
        User user = new User("ada@example.test", "hash", "Ada", "Lovelace");
        Plan plan = new Plan("PRO", "Pro", new BigDecimal("29.00"), true);
        Subscription subscription = new Subscription(user, plan, now, now.plusSeconds(86400));
        return new Invoice(subscription, new BigDecimal("29.00"), now, now.plusSeconds(86400));
    }
}
