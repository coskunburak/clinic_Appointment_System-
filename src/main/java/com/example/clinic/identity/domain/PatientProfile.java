package com.example.clinic.identity.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Concrete patient profile entity extending the shared {@link PersonProfile} abstraction.
 * Preserves the PostgreSQL V1 {@code patient_profile} table mapping without schema modifications.
 */
@Entity
@Table(name = "patient_profile")
public class PatientProfile extends PersonProfile {

    protected PatientProfile() {
        super();
    }

    public PatientProfile(UserAccount userAccount, String fullName, String phone) {
        this(UUID.randomUUID(), userAccount, fullName, phone, Instant.now());
    }

    public PatientProfile(UUID id, UserAccount userAccount, String fullName, String phone, Instant createdAt) {
        super(id, userAccount, fullName, phone, createdAt);
    }

    @Override
    public String toString() {
        return "PatientProfile{" +
                "id=" + getId() +
                ", fullName='" + getFullName() + '\'' +
                ", phone='" + getPhone() + '\'' +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}
