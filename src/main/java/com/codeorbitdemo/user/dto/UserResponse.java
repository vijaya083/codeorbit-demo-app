package com.codeorbitdemo.user.dto;
import com.codeorbitdemo.user.User;
import java.time.Instant;
public record UserResponse(Long id, String email, String firstName, String lastName, boolean active, Instant createdAt) {
    public static UserResponse from(User user) { return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.isActive(), user.getCreatedAt()); }
}
