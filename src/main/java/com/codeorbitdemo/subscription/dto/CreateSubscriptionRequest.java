package com.codeorbitdemo.subscription.dto;
import jakarta.validation.constraints.NotNull;
public record CreateSubscriptionRequest(@NotNull Long userId, @NotNull Long planId) { }
