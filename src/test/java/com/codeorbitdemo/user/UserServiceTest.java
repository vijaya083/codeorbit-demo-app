package com.codeorbitdemo.user;

import com.codeorbitdemo.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository users;
    @Mock NotificationService notifications;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void registersUserAndStoresPasswordHash() {
        UserService service = new UserService(users, encoder, notifications);
        when(users.existsByEmailIgnoreCase("ada@example.test")).thenReturn(false);
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.register("Ada@Example.Test", "password123", "Ada", "Lovelace");

        assertEquals("ada@example.test", response.email());
        ArgumentCaptor<User> saved = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertNotEquals("password123", saved.getValue().getPasswordHash());
        assertTrue(encoder.matches("password123", saved.getValue().getPasswordHash()));
        verify(notifications).registrationCompleted(saved.getValue());
    }

    @Test
    void rejectsDuplicateEmail() {
        UserService service = new UserService(users, encoder, notifications);
        when(users.existsByEmailIgnoreCase("ada@example.test")).thenReturn(true);

        var error = assertThrows(com.codeorbitdemo.common.exception.ApiException.class,
                () -> service.register("ada@example.test", "password123", "Ada", "Lovelace"));

        assertEquals(org.springframework.http.HttpStatus.CONFLICT, error.status());
        verify(users, never()).save(any());
        verifyNoInteractions(notifications);
    }
}
