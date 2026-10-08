package com.codeorbitdemo.notification;

import com.codeorbitdemo.subscription.Subscription;
import com.codeorbitdemo.user.User;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final EmailService emailService;
    public NotificationService(EmailService emailService) { this.emailService = emailService; }
    public void registrationCompleted(User user) { emailService.send(user.getEmail(), "Welcome to CodeOrbit Demo"); }
    public void subscriptionCreated(Subscription subscription) { emailService.send(subscription.getUser().getEmail(), "Your subscription is active"); }
    public void passwordResetRequested(User user, String token) {
        emailService.send(user.getEmail(), "Password reset", "Use this one-time token to reset your password: " + token);
    }
}
