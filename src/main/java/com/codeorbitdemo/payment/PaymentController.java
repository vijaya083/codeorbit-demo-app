package com.codeorbitdemo.payment;
import com.codeorbitdemo.payment.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api")
public class PaymentController {
    private final PaymentService payments;
    public PaymentController(PaymentService payments) { this.payments = payments; }
    @PostMapping("/invoices/{invoiceId}/payments") @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse pay(@PathVariable Long invoiceId, @Valid @RequestBody RecordPaymentRequest request) { return payments.recordPayment(invoiceId, request.amount(), request.simulateFailure()); }

    @PostMapping("/payments/{paymentId}/retry") @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse retry(@PathVariable Long paymentId, @RequestBody RetryPaymentRequest request) {
        return payments.retryPayment(paymentId, request.simulateFailure());
    }
}
