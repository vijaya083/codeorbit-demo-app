package com.codeorbitdemo.payment;

import com.codeorbitdemo.billing.Invoice;
import com.codeorbitdemo.billing.InvoiceService;
import com.codeorbitdemo.billing.InvoiceStatus;
import com.codeorbitdemo.common.exception.ApiException;
import com.codeorbitdemo.notification.NotificationService;
import com.codeorbitdemo.payment.dto.PaymentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class PaymentService {
    private final PaymentRepository payments;
    private final InvoiceService invoices;
    private final NotificationService notifications;

    public PaymentService(PaymentRepository payments, InvoiceService invoices, NotificationService notifications) {
        this.payments = payments;
        this.invoices = invoices;
        this.notifications = notifications;
    }

    @Transactional
    public PaymentResponse recordPayment(Long invoiceId, BigDecimal amount) {
        return recordPayment(invoiceId, amount, false);
    }

    @Transactional
    public PaymentResponse recordPayment(Long invoiceId, BigDecimal amount, boolean simulateFailure) {
        Invoice invoice = invoices.requireInvoice(invoiceId);
        return attemptPayment(invoice, amount, simulateFailure);
    }

    @Transactional
    public PaymentResponse retryPayment(Long failedPaymentId, boolean simulateFailure) {
        Payment failedPayment = payments.findById(failedPaymentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found"));
        if (failedPayment.getStatus() != PaymentStatus.FAILED)
            throw new ApiException(HttpStatus.CONFLICT, "Only failed payments can be retried");

        Invoice invoice = invoices.requireInvoice(failedPayment.getInvoice().getId());
        if (invoice.getStatus() != InvoiceStatus.OPEN)
            throw new ApiException(HttpStatus.CONFLICT, "Invoice is not open for payment");

        return attemptPayment(invoice, failedPayment.getAmount(), simulateFailure);
    }

    private PaymentResponse attemptPayment(Invoice invoice, BigDecimal amount, boolean simulateFailure) {
        if (amount == null || amount.signum() <= 0 || amount.compareTo(invoice.getAmount()) != 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Payment amount must equal the invoice amount and be positive");
        if (invoice.getStatus() != InvoiceStatus.OPEN)
            throw new ApiException(HttpStatus.CONFLICT, "Invoice is not open for payment");

        PaymentStatus outcome = simulateFailure ? PaymentStatus.FAILED : PaymentStatus.SUCCEEDED;
        Payment payment = payments.save(new Payment(invoice, amount, outcome));
        if (outcome == PaymentStatus.SUCCEEDED) {
            invoices.markPaid(invoice);
        } else {
            notifications.paymentFailed(invoice);
        }
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public Payment requireSuccessful(Long paymentId) {
        Payment payment = payments.findById(paymentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found"));
        if (payment.getStatus() != PaymentStatus.SUCCEEDED)
            throw new ApiException(HttpStatus.CONFLICT, "Payment did not succeed");
        return payment;
    }
}
