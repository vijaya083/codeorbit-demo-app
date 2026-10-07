package com.codeorbitdemo.billing;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findAllBySubscriptionUserIdOrderByIssuedAtDesc(Long userId);
    Optional<Invoice> findById(Long id);
}
