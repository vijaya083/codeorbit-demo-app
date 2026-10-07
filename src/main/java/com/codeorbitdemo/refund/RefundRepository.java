package com.codeorbitdemo.refund;
import org.springframework.data.jpa.repository.JpaRepository;
import java.math.BigDecimal;
public interface RefundRepository extends JpaRepository<Refund, Long> {
    @org.springframework.data.jpa.repository.Query("select coalesce(sum(r.amount), 0) from Refund r where r.payment.id = :paymentId and r.status = com.codeorbitdemo.refund.RefundStatus.COMPLETED")
    BigDecimal sumCompletedAmount(Long paymentId);
}
