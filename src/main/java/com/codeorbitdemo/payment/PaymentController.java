package com.codeorbitdemo.payment;
import com.codeorbitdemo.payment.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/invoices")
public class PaymentController {
    private final PaymentService payments;
    public PaymentController(PaymentService payments) { this.payments = payments; }
    @PostMapping("/{invoiceId}/payments") @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse pay(@PathVariable Long invoiceId, @Valid @RequestBody RecordPaymentRequest request) { return payments.recordPayment(invoiceId, request.amount(), request.simulateFailure()); }
}
