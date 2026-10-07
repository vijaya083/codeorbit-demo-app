package com.codeorbitdemo.subscription;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class PlanServiceRepositoryTest {
    @Autowired PlanRepository plans;

    @Test
    void activePlanListingExcludesInactivePlans() {
        plans.save(new Plan("ACTIVE", "Active", new BigDecimal("12.00"), true));
        plans.save(new Plan("INACTIVE", "Inactive", new BigDecimal("5.00"), false));
        PlanService service = new PlanService(plans);

        var result = service.activePlans();

        assertEquals(1, result.size());
        assertEquals("ACTIVE", result.get(0).getCode());
    }
}
