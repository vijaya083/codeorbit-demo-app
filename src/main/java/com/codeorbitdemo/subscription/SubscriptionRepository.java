package com.codeorbitdemo.subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    boolean existsByUserIdAndStatus(Long userId, SubscriptionStatus status);
    List<Subscription> findAllByUserIdOrderByStartedAtDesc(Long userId);
}
