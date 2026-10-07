package com.codeorbitdemo.subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface PlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findAllByActiveTrueOrderByMonthlyPriceAsc();
    Optional<Plan> findByIdAndActiveTrue(Long id);
    boolean existsByCode(String code);
}
