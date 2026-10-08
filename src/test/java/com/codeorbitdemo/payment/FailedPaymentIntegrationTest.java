package com.codeorbitdemo.payment;

import com.codeorbitdemo.notification.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FailedPaymentIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoSpyBean EmailService emailService;

    @Test
    void failedAttemptRemainsOpenAndSendsInvoiceOnlyNotification() throws Exception {
        String email = "failed-payment-" + System.nanoTime() + "@example.test";
        String registration = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"password123\",\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}".formatted(email)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long userId = mapper.readTree(registration).get("id").asLong();

        JsonNode plans = mapper.readTree(mvc.perform(get("/api/plans")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        long planId = plans.get(0).get("id").asLong();
        mvc.perform(post("/api/subscriptions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":%d,\"planId\":%d}".formatted(userId, planId)))
                .andExpect(status().isCreated());

        JsonNode invoice = mapper.readTree(mvc.perform(get("/api/users/{userId}/invoices", userId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get(0);
        long invoiceId = invoice.get("id").asLong();
        String amount = invoice.get("amount").asText();

        String result = mvc.perform(post("/api/invoices/{invoiceId}/payments", invoiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":%s,\"simulateFailure\":true}".formatted(amount)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("FAILED"))
                .andReturn().getResponse().getContentAsString();
        JsonNode payment = mapper.readTree(result);
        assertEquals(invoiceId, payment.get("invoiceId").asLong());

        mvc.perform(get("/api/users/{userId}/invoices", userId))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("OPEN"));
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(emailService).send(eq(email), eq("Payment failed"), message.capture());
        assertEquals("Payment for invoice #%d failed. Please review this invoice.".formatted(invoiceId), message.getValue());
        assertFalse(message.getValue().contains(amount));
    }
}
