package com.codeorbitdemo.payment;

import com.codeorbitdemo.billing.Invoice;
import com.codeorbitdemo.billing.InvoiceService;
import com.codeorbitdemo.billing.InvoiceStatus;
import com.codeorbitdemo.common.exception.ApiException;
import com.codeorbitdemo.payment.dto.PaymentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class PaymentService {
    private final PaymentRepository payments; private final InvoiceService invoices;
    public PaymentService(PaymentRepository payments, InvoiceService invoices) { this.payments = payments; this.invoices = invoices; }
    @Transactional public PaymentResponse recordPayment(Long invoiceId, BigDecimal amount) {
        Invoice invoice = invoices.requireInvoice(invoiceId);
        if (amount == null || amount.signum() <= 0 || amount.compareTo(invoice.getAmount()) != 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Payment amount must equal the invoice amount and be positive");
        if (invoice.getStatus() != InvoiceStatus.OPEN) throw new ApiException(HttpStatus.CONFLICT, "Invoice is not open for payment");
        Payment payment = payments.save(new Payment(invoice, amount)); invoices.markPaid(invoice);
        return PaymentResponse.from(payment);
    }
    @Transactional(readOnly = true) public Payment requireSuccessful(Long paymentId) {
        Payment payment = payments.findById(paymentId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found"));
        if (payment.getStatus() != PaymentStatus.SUCCEEDED) throw new ApiException(HttpStatus.CONFLICT, "Payment did not succeed");
        return payment;
    }
}
