package com.codeorbitdemo.subscription;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
@Component
public class PlanDataInitializer implements CommandLineRunner {
    private final PlanRepository plans;
    public PlanDataInitializer(PlanRepository plans) { this.plans = plans; }
    @Override @Transactional public void run(String... args) {
        add("BASIC", "Basic", "9.00"); add("PRO", "Pro", "29.00"); add("BUSINESS", "Business", "99.00");
    }
    private void add(String code, String name, String price) {
        if (!plans.existsByCode(code)) plans.save(new Plan(code, name, new BigDecimal(price), true));
    }
}
