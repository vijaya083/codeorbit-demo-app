package com.codeorbitdemo.subscription;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/plans")
public class PlanController {
    private final PlanService planService;
    public PlanController(PlanService planService) { this.planService = planService; }
    @GetMapping public List<PlanResponse> list() { return planService.activePlans().stream().map(PlanResponse::from).toList(); }
    public record PlanResponse(Long id, String code, String name, java.math.BigDecimal monthlyPrice) {
        static PlanResponse from(Plan plan) { return new PlanResponse(plan.getId(), plan.getCode(), plan.getName(), plan.getMonthlyPrice()); }
    }
}
