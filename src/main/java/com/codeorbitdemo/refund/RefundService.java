package com.codeorbitdemo.refund;

import com.codeorbitdemo.common.exception.ApiException;
import com.codeorbitdemo.payment.Payment;
import com.codeorbitdemo.payment.PaymentService;
import com.codeorbitdemo.refund.dto.RefundResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class RefundService {
    private final RefundRepository refunds; private final PaymentService payments;
    public RefundService(RefundRepository refunds, PaymentService payments) { this.refunds = refunds; this.payments = payments; }
    @Transactional public RefundResponse refund(Long paymentId, BigDecimal amount) {
        Payment payment = payments.requireSuccessful(paymentId);
        BigDecimal alreadyRefunded = refunds.sumCompletedAmount(paymentId);
        if (amount == null || amount.signum() <= 0 || amount.compareTo(payment.getAmount().subtract(alreadyRefunded)) > 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Refund amount exceeds the refundable balance or is invalid");
        return RefundResponse.from(refunds.save(new Refund(payment, amount)));
    }
}
