package com.codeorbitdemo.payment;

import com.codeorbitdemo.billing.Invoice;
import com.codeorbitdemo.billing.InvoiceService;
import com.codeorbitdemo.common.exception.ApiException;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock PaymentRepository payments;
    @Mock InvoiceService invoices;

    @Test
    void recordsSuccessfulPaymentAndMarksInvoicePaid() {
        Invoice invoice = invoice();
        when(invoices.requireInvoice(8L)).thenReturn(invoice);
        when(payments.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> { invoice.markPaid(); return null; }).when(invoices).markPaid(invoice);

        var response = new PaymentService(payments, invoices).recordPayment(8L, new BigDecimal("29.00"));

        assertEquals(PaymentStatus.SUCCEEDED, response.status());
        assertEquals(new BigDecimal("29.00"), response.amount());
        assertEquals(com.codeorbitdemo.billing.InvoiceStatus.PAID, invoice.getStatus());
        verify(invoices).markPaid(invoice);
    }

    @Test
    void rejectsNonPositiveOrMismatchedInvoiceAmounts() {
        Invoice invoice = invoice();
        when(invoices.requireInvoice(8L)).thenReturn(invoice);
        PaymentService service = new PaymentService(payments, invoices);

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
                () -> new PaymentService(payments, invoices).recordPayment(8L, new BigDecimal("29.00")));

        assertEquals(HttpStatus.CONFLICT, error.status());
        verifyNoInteractions(payments);
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
