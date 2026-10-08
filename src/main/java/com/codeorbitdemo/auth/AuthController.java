package com.codeorbitdemo.auth;

import com.codeorbitdemo.auth.dto.*;
import com.codeorbitdemo.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request.email(), request.password(), request.firstName(), request.lastName());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request.email());
    }

    @PostMapping("/password-reset/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void completePasswordReset(@Valid @RequestBody PasswordResetCompletionRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
    }
}
