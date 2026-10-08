package com.codeorbitdemo.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetCompletionRequest(@NotBlank String token,
                                             @NotBlank @Size(min = 8, max = 100) String newPassword) { }
