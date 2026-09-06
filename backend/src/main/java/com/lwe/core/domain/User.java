package com.lwe.core.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Benutzerkonto. Primäre Entität — jeder User besitzt Welten, kann Mitglied sein und Charaktere steuern.
 *
 * <p>Passwort wird als BCrypt-Hash gespeichert. {@code locale} wird bei Registration via
 * {@code Accept-Language} gesetzt (siehe {@code docs/ADR/007-internationalization-strategy.md}).
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 200)
    private String email;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 200)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String role = "USER";

    @Column(nullable = false, length = 10)
    private String locale = "de";

    @Column(name = "plan_id")
    private UUID planId;

    @Column(name = "plan_expires_at")
    private Instant planExpiresAt;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "verification_token", length = 200)
    private String verificationToken;

    @Column(name = "verification_token_expires_at")
    private Instant verificationTokenExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected User() {}

    public User(String email, String username, String passwordHash, String role, String locale) {
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.locale = locale;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String v) { this.passwordHash = v; }
    public String getRole() { return role; }
    public String getLocale() { return locale; }
    public void setLocale(String v) { this.locale = v; }
    public UUID getPlanId() { return planId; }
    public void setPlanId(UUID v) { this.planId = v; }
    public Instant getPlanExpiresAt() { return planExpiresAt; }
    public void setPlanExpiresAt(Instant v) { this.planExpiresAt = v; }
    public void setRole(String v) { this.role = v; }
    public Instant getEmailVerifiedAt() { return emailVerifiedAt; }
    public void setEmailVerifiedAt(Instant v) { this.emailVerifiedAt = v; }
    public String getVerificationToken() { return verificationToken; }
    public void setVerificationToken(String v) { this.verificationToken = v; }
    public Instant getVerificationTokenExpiresAt() { return verificationTokenExpiresAt; }
    public void setVerificationTokenExpiresAt(Instant v) { this.verificationTokenExpiresAt = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}