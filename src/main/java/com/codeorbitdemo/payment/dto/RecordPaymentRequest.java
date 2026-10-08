package com.codeorbitdemo.payment.dto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
public record RecordPaymentRequest(@NotNull @DecimalMin(value = "0.01") BigDecimal amount,
                                   boolean simulateFailure) { }
