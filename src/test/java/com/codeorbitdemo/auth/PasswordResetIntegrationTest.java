package com.codeorbitdemo.auth;

import com.codeorbitdemo.notification.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PasswordResetIntegrationTest {
    private static final String RESET_BODY_PREFIX = "Use this one-time token to reset your password: ";
    @Autowired MockMvc mvc;
    @MockitoSpyBean EmailService emailService;

    @Test
    void resetTokenChangesPasswordAndCanOnlyBeUsedOnce() throws Exception {
        String email = "reset-" + System.nanoTime() + "@example.test";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"old-password-123\",\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}".formatted(email)))
                .andExpect(status().isCreated());

        String resetResponse = mvc.perform(post("/api/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(email)))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        assertTrue(resetResponse.isBlank());

        ArgumentCaptor<String> deliveredBody = ArgumentCaptor.forClass(String.class);
        verify(emailService).send(eq(email), eq("Password reset"), deliveredBody.capture());
        assertTrue(deliveredBody.getValue().startsWith(RESET_BODY_PREFIX));
        String token = deliveredBody.getValue().substring(RESET_BODY_PREFIX.length());
        assertFalse(token.isBlank());

        mvc.perform(post("/api/auth/password-reset/complete").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"invalid-token\",\"newPassword\":\"new-password-456\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/password-reset/complete").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\",\"newPassword\":\"new-password-456\"}".formatted(token)))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"old-password-123\"}".formatted(email)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"new-password-456\"}".formatted(email)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/password-reset/complete").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\",\"newPassword\":\"third-password-789\"}".formatted(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetRequestForUnknownAccountReturnsSameAcceptedResponseWithoutNotification() throws Exception {
        String response = mvc.perform(post("/api/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody-" + System.nanoTime() + "@example.test\"}"))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();

        assertTrue(response.isBlank());
        verify(emailService, never()).send(anyString(), eq("Password reset"), anyString());
    }
}
