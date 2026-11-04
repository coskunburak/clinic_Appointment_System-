package com.example.clinic.identity.domain;

import com.example.clinic.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_account")
public class UserAccount extends BaseEntity {

    @NotBlank
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected UserAccount() {
        super();
    }

    public UserAccount(String email, String passwordHash) {
        this(UUID.randomUUID(), email, passwordHash, true, Instant.now());
    }

    public UserAccount(UUID id, String email, String passwordHash, boolean enabled, Instant createdAt) {
        super(id, createdAt);
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        this.enabled = enabled;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = Objects.requireNonNull(email, "email must not be null");
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "UserAccount{" +
                "id=" + getId() +
                ", email='" + email + '\'' +
                ", enabled=" + enabled +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}
