package com.codeorbitdemo.subscription;
import com.codeorbitdemo.subscription.dto.CreateSubscriptionRequest;
import com.codeorbitdemo.subscription.dto.SubscriptionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {
    private final SubscriptionService service;
    public SubscriptionController(SubscriptionService service) { this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public SubscriptionResponse create(@Valid @RequestBody CreateSubscriptionRequest request) { return service.create(request.userId(), request.planId()); }
    @PostMapping("/{id}/cancel") public SubscriptionResponse cancel(@PathVariable Long id) { return service.cancel(id); }
}
