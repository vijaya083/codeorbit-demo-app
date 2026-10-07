package com.codeorbitdemo.auth;

import com.codeorbitdemo.common.exception.ApiException;
import com.codeorbitdemo.user.User;
import com.codeorbitdemo.user.UserRepository;
import com.codeorbitdemo.user.UserService;
import com.codeorbitdemo.user.dto.UserResponse;
import com.codeorbitdemo.auth.dto.AuthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserService userService; private final UserRepository users; private final PasswordEncoder passwordEncoder; private final TokenService tokens;
    public AuthService(UserService userService, UserRepository users, PasswordEncoder passwordEncoder, TokenService tokens) { this.userService = userService; this.users = users; this.passwordEncoder = passwordEncoder; this.tokens = tokens; }
    @Transactional public UserResponse register(String email, String password, String firstName, String lastName) { return userService.register(email, password, firstName, lastName); }
    @Transactional(readOnly = true)
    public AuthResponse login(String email, String password) {
        User user = users.findByEmailIgnoreCase(email.trim()).filter(User::isActive)
                .filter(candidate -> passwordEncoder.matches(password, candidate.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return new AuthResponse(tokens.issueToken(), UserResponse.from(user));
    }
}
