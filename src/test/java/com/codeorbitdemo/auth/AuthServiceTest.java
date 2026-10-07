package com.codeorbitdemo.auth;

import com.codeorbitdemo.common.exception.ApiException;
import com.codeorbitdemo.user.User;
import com.codeorbitdemo.user.UserRepository;
import com.codeorbitdemo.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserService userService;
    @Mock UserRepository users;
    @Mock PasswordEncoder passwordEncoder;
    @Mock TokenService tokens;

    @Test
    void logsInWithValidCredentialsAndIssuesToken() {
        User user = user("password-hash");
        when(users.findByEmailIgnoreCase("Ada@Example.Test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "password-hash")).thenReturn(true);
        when(tokens.issueToken()).thenReturn("demo-token");

        var result = service().login("Ada@Example.Test", "password123");

        assertEquals("demo-token", result.token());
        assertEquals("ada@example.test", result.user().email());
        verify(tokens).issueToken();
    }

    @Test
    void rejectsInvalidPassword() {
        when(users.findByEmailIgnoreCase("ada@example.test")).thenReturn(Optional.of(user("password-hash")));
        when(passwordEncoder.matches("wrong-password", "password-hash")).thenReturn(false);

        var error = assertThrows(ApiException.class, () -> service().login("ada@example.test", "wrong-password"));

        assertEquals(HttpStatus.UNAUTHORIZED, error.status());
        verifyNoInteractions(tokens);
    }

    @Test
    void rejectsUnknownUser() {
        when(users.findByEmailIgnoreCase("unknown@example.test")).thenReturn(Optional.empty());

        var error = assertThrows(ApiException.class, () -> service().login("unknown@example.test", "password123"));

        assertEquals(HttpStatus.UNAUTHORIZED, error.status());
        assertEquals("Invalid email or password", error.getMessage());
        verifyNoInteractions(passwordEncoder, tokens);
    }

    private AuthService service() { return new AuthService(userService, users, passwordEncoder, tokens); }
    private static User user(String passwordHash) { return new User("ada@example.test", passwordHash, "Ada", "Lovelace"); }
}
