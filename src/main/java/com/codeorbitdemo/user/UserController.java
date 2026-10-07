package com.codeorbitdemo.user;
import com.codeorbitdemo.subscription.SubscriptionService;
import com.codeorbitdemo.subscription.dto.SubscriptionResponse;
import com.codeorbitdemo.billing.InvoiceService;
import com.codeorbitdemo.billing.dto.InvoiceResponse;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final SubscriptionService subscriptions; private final InvoiceService invoices;
    public UserController(SubscriptionService subscriptions, InvoiceService invoices) { this.subscriptions = subscriptions; this.invoices = invoices; }
    @GetMapping("/{userId}/subscriptions") public List<SubscriptionResponse> subscriptions(@PathVariable Long userId) { return subscriptions.forUser(userId); }
    @GetMapping("/{userId}/invoices") public List<InvoiceResponse> invoices(@PathVariable Long userId) { return invoices.forUser(userId); }
}
