package com.codeorbitdemo.auth;

import com.codeorbitdemo.auth.dto.AuthResponse;
import com.codeorbitdemo.common.exception.ApiException;
import com.codeorbitdemo.notification.NotificationService;
import com.codeorbitdemo.user.User;
import com.codeorbitdemo.user.UserRepository;
import com.codeorbitdemo.user.UserService;
import com.codeorbitdemo.user.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {
    private final UserService userService;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final PasswordResetTokenRepository resetTokens;
    private final NotificationService notifications;

    public AuthService(UserService userService, UserRepository users, PasswordEncoder passwordEncoder,
                       TokenService tokens, PasswordResetTokenRepository resetTokens,
                       NotificationService notifications) {
        this.userService = userService;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.resetTokens = resetTokens;
        this.notifications = notifications;
    }

    @Transactional
    public UserResponse register(String email, String password, String firstName, String lastName) {
        return userService.register(email, password, firstName, lastName);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(String email, String password) {
        User user = users.findByEmailIgnoreCase(email.trim()).filter(User::isActive)
                .filter(candidate -> passwordEncoder.matches(password, candidate.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return new AuthResponse(tokens.issueToken(), UserResponse.from(user));
    }

    @Transactional
    public void requestPasswordReset(String email) {
        users.findByEmailIgnoreCase(email.trim()).filter(User::isActive).ifPresent(user -> {
            String rawToken = tokens.issueToken();
            String tokenHash = tokens.hashToken(rawToken);
            resetTokens.save(new PasswordResetToken(user, tokenHash, Instant.now().plus(30, ChronoUnit.MINUTES)));
            notifications.passwordResetRequested(user, rawToken);
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        Instant now = Instant.now();
        PasswordResetToken resetToken = resetTokens.findByTokenHash(tokens.hashToken(rawToken))
                .filter(token -> token.isUsableAt(now) && token.getUser().isActive())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired password reset token"));
        User user = resetToken.getUser();
        user.changePasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);
        resetToken.consumeAt(now);
        resetTokens.save(resetToken);
    }
}
