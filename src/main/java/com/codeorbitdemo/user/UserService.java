package com.codeorbitdemo.user;

import com.codeorbitdemo.common.exception.ApiException;
import com.codeorbitdemo.notification.NotificationService;
import com.codeorbitdemo.user.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notifications;
    public UserService(UserRepository users, PasswordEncoder passwordEncoder, NotificationService notifications) { this.users = users; this.passwordEncoder = passwordEncoder; this.notifications = notifications; }
    @Transactional
    public UserResponse register(String email, String password, String firstName, String lastName) {
        String normalized = email.trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(normalized)) throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        User user = users.save(new User(normalized, passwordEncoder.encode(password), firstName, lastName));
        notifications.registrationCompleted(user);
        return UserResponse.from(user);
    }
    @Transactional(readOnly = true)
    public User requireUser(Long id) { return users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found")); }
}
