package com.codeorbitdemo.subscription;
import com.codeorbitdemo.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
public class PlanService {
    private final PlanRepository plans;
    public PlanService(PlanRepository plans) { this.plans = plans; }
    @Transactional(readOnly = true) public List<Plan> activePlans() { return plans.findAllByActiveTrueOrderByMonthlyPriceAsc(); }
    @Transactional(readOnly = true) public Plan requireActive(Long id) { return plans.findById(id).map(plan -> {
        if (!plan.isActive()) throw new ApiException(HttpStatus.CONFLICT, "Plan is inactive"); return plan;
    }).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Plan not found")); }
}
