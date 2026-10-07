package com.codeorbitdemo.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    public void send(String recipient, String subject) {
        String sanitizedRecipient = recipient == null ? "unknown" : recipient.replaceAll("[\\r\\n]", "");
        String sanitizedSubject = subject == null ? "" : subject.replaceAll("[\\r\\n]", "");
        log.info("Simulated email delivery to {} with subject '{}'", sanitizedRecipient, sanitizedSubject);
    }
}
