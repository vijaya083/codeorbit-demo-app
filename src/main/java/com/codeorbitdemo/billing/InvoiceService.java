package com.codeorbitdemo.billing;
import com.codeorbitdemo.billing.dto.InvoiceResponse;
import com.codeorbitdemo.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
@Service
public class InvoiceService {
    private final InvoiceRepository invoices;
    public InvoiceService(InvoiceRepository invoices) { this.invoices = invoices; }
    @Transactional public Invoice createInitialInvoice(com.codeorbitdemo.subscription.Subscription subscription) {
        Instant issued = Instant.now(); return invoices.save(new Invoice(subscription, subscription.getPlan().getMonthlyPrice(), issued, subscription.getCurrentPeriodEnd()));
    }
    @Transactional(readOnly = true) public Invoice requireInvoice(Long id) { return invoices.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invoice not found")); }
    @Transactional(readOnly = true) public List<InvoiceResponse> forUser(Long userId) { return invoices.findAllBySubscriptionUserIdOrderByIssuedAtDesc(userId).stream().map(InvoiceResponse::from).toList(); }
    @Transactional public void markPaid(Invoice invoice) { invoice.markPaid(); invoices.save(invoice); }
}
