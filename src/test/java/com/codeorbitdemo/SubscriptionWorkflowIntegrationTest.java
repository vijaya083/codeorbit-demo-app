package com.codeorbitdemo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
class SubscriptionWorkflowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void registrationSubscriptionBillingPaymentAndRefundFlow() throws Exception {
        String email = "user-" + System.nanoTime() + "@example.test";
        String registration = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"password123\",\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}".formatted(email)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.passwordHash").doesNotExist()).andReturn().getResponse().getContentAsString();
        long userId = mapper.readTree(registration).get("id").asLong();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"password123\",\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}".formatted(email)))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"password123\"}".formatted(email)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"wrong-password\"}".formatted(email)))
                .andExpect(status().isUnauthorized());

        String plansBody = mvc.perform(get("/api/plans")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode plans = mapper.readTree(plansBody);
        assertEquals(3, plans.size());
        long planId = plans.get(0).get("id").asLong();
        String subscriptionBody = mvc.perform(post("/api/subscriptions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":%d,\"planId\":%d}".formatted(userId, planId)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ACTIVE")).andReturn().getResponse().getContentAsString();
        long subscriptionId = mapper.readTree(subscriptionBody).get("id").asLong();
        mvc.perform(post("/api/subscriptions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":%d,\"planId\":%d}".formatted(userId, planId)))
                .andExpect(status().isConflict());

        String invoicesBody = mvc.perform(get("/api/users/{id}/invoices", userId)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode invoices = mapper.readTree(invoicesBody);
        assertEquals(1, invoices.size());
        long invoiceId = invoices.get(0).get("id").asLong();
        String amount = invoices.get(0).get("amount").asText();
        assertEquals(subscriptionId, invoices.get(0).get("subscriptionId").asLong());

        mvc.perform(post("/api/invoices/{id}/payments", invoiceId).contentType(MediaType.APPLICATION_JSON).content("{\"amount\":1.00}"))
                .andExpect(status().isBadRequest());
        String paymentBody = mvc.perform(post("/api/invoices/{id}/payments", invoiceId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":%s}".formatted(amount)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("SUCCEEDED")).andReturn().getResponse().getContentAsString();
        long paymentId = mapper.readTree(paymentBody).get("id").asLong();
        mvc.perform(get("/api/users/{id}/invoices", userId)).andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("PAID"));
        mvc.perform(post("/api/invoices/{id}/payments", invoiceId).contentType(MediaType.APPLICATION_JSON).content("{\"amount\":9.00}"))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/payments/{id}/refunds", paymentId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":%s}".formatted(amount)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(post("/api/payments/{id}/refunds", paymentId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1.00}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/subscriptions/{id}/cancel", subscriptionId)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(get("/api/users/{id}/subscriptions", userId)).andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("CANCELLED"));
        mvc.perform(post("/api/subscriptions/{id}/cancel", subscriptionId)).andExpect(status().isConflict());
    }
}
