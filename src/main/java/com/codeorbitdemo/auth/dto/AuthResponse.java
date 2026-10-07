package com.codeorbitdemo.auth.dto;
import com.codeorbitdemo.user.dto.UserResponse;
public record AuthResponse(String token, UserResponse user) { }
