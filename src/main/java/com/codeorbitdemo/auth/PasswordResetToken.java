package com.codeorbitdemo.auth;

import com.codeorbitdemo.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private User user;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(nullable = false) private Instant expiresAt;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    private Instant consumedAt;

    protected PasswordResetToken() { }
    public PasswordResetToken(User user, String tokenHash, Instant expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }
    public User getUser() { return user; }
    public String getTokenHash() { return tokenHash; }
    public boolean isUsableAt(Instant now) { return consumedAt == null && expiresAt.isAfter(now); }
    public void consumeAt(Instant now) { this.consumedAt = now; }
}
