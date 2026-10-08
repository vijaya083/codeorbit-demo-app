package com.codeorbitdemo.auth;

import com.codeorbitdemo.common.exception.ApiException;
import com.codeorbitdemo.notification.NotificationService;
import com.codeorbitdemo.user.User;
import com.codeorbitdemo.user.UserRepository;
import com.codeorbitdemo.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserService userService;
    @Mock UserRepository users;
    @Mock PasswordEncoder passwordEncoder;
    @Mock TokenService tokens;
    @Mock PasswordResetTokenRepository resetTokens;
    @Mock NotificationService notifications;

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

    @Test
    void requestsResetForActiveUserAndSendsOnlyRawTokenToNotification() {
        User user = user("password-hash");
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(tokens.issueToken()).thenReturn("raw-reset-token");
        when(tokens.hashToken("raw-reset-token")).thenReturn("hashed-reset-token");
        when(resetTokens.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service().requestPasswordReset(user.getEmail());

        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(resetTokens).save(saved.capture());
        assertEquals("hashed-reset-token", saved.getValue().getTokenHash());
        assertNotEquals("raw-reset-token", saved.getValue().getTokenHash());
        verify(notifications).passwordResetRequested(user, "raw-reset-token");
    }

    @Test
    void resetChangesPasswordHashAndConsumesToken() {
        User user = user("old-hash");
        PasswordResetToken resetToken = new PasswordResetToken(user, "hashed-reset-token", Instant.now().plusSeconds(600));
        when(tokens.hashToken("raw-reset-token")).thenReturn("hashed-reset-token");
        when(resetTokens.findByTokenHash("hashed-reset-token")).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("new-password-123")).thenReturn("new-hash");
        when(users.save(user)).thenReturn(user);
        when(resetTokens.save(resetToken)).thenReturn(resetToken);

        service().resetPassword("raw-reset-token", "new-password-123");

        assertEquals("new-hash", user.getPasswordHash());
        assertFalse(resetToken.isUsableAt(Instant.now()));
        verify(users).save(user);
        verify(resetTokens).save(resetToken);
    }

    @Test
    void rejectsExpiredAndUnknownResetTokens() {
        when(tokens.hashToken("missing-token")).thenReturn("missing-hash");
        when(resetTokens.findByTokenHash("missing-hash")).thenReturn(Optional.empty());
        when(tokens.hashToken("expired-token")).thenReturn("expired-hash");
        when(resetTokens.findByTokenHash("expired-hash")).thenReturn(Optional.of(
                new PasswordResetToken(user("password-hash"), "expired-hash", Instant.now().minusSeconds(1))));

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class,
                () -> service().resetPassword("missing-token", "new-password-123")).status());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class,
                () -> service().resetPassword("expired-token", "new-password-123")).status());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void doesNotIssueResetForUnknownAccount() {
        when(users.findByEmailIgnoreCase("missing@example.test")).thenReturn(Optional.empty());

        service().requestPasswordReset("missing@example.test");

        verifyNoInteractions(tokens, resetTokens, notifications);
    }

    private AuthService service() {
        return new AuthService(userService, users, passwordEncoder, tokens, resetTokens, notifications);
    }
    private static User user(String passwordHash) { return new User("ada@example.test", passwordHash, "Ada", "Lovelace"); }
}
