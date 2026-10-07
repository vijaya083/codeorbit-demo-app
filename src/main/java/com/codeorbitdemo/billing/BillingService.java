package com.codeorbitdemo.billing;
import com.codeorbitdemo.subscription.Subscription;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class BillingService {
    private final InvoiceService invoiceService;
    public BillingService(InvoiceService invoiceService) { this.invoiceService = invoiceService; }
    @Transactional public Invoice createInitialInvoice(Subscription subscription) { return invoiceService.createInitialInvoice(subscription); }
}
