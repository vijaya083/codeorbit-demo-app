package com.codeorbitdemo.subscription;

import com.codeorbitdemo.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {
    @Mock PlanRepository plans;

    @Test
    void returnsActivePlansInRepositoryOrder() {
        List<Plan> active = List.of(new Plan("BASIC", "Basic", new BigDecimal("9.00"), true),
                new Plan("PRO", "Pro", new BigDecimal("29.00"), true));
        when(plans.findAllByActiveTrueOrderByMonthlyPriceAsc()).thenReturn(active);

        assertEquals(active, new PlanService(plans).activePlans());
        verify(plans).findAllByActiveTrueOrderByMonthlyPriceAsc();
    }

    @Test
    void rejectsAnInactivePlanWhenRequired() {
        when(plans.findById(7L)).thenReturn(Optional.of(new Plan("OLD", "Old plan", BigDecimal.ONE, false)));

        var error = assertThrows(ApiException.class, () -> new PlanService(plans).requireActive(7L));

        assertEquals(HttpStatus.CONFLICT, error.status());
    }
}
