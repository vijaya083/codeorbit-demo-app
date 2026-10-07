package com.codeorbitdemo.refund;
import com.codeorbitdemo.refund.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/payments")
public class RefundController {
    private final RefundService refunds;
    public RefundController(RefundService refunds) { this.refunds = refunds; }
    @PostMapping("/{paymentId}/refunds") @ResponseStatus(HttpStatus.CREATED)
    public RefundResponse refund(@PathVariable Long paymentId, @Valid @RequestBody RefundRequest request) { return refunds.refund(paymentId, request.amount()); }
}
