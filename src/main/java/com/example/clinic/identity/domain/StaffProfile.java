package com.example.clinic.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Concrete medical/clinic staff profile entity extending the shared {@link PersonProfile} abstraction.
 * Encapsulates staff-specific attributes such as staff type, medical specialty, and active status.
 */
@Entity
@Table(name = "staff_profile")
public class StaffProfile extends PersonProfile {

    @NotBlank
    @Column(name = "staff_type", nullable = false, length = 50)
    private String staffType;

    @Column(name = "specialty", length = 100)
    private String specialty;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected StaffProfile() {
        super();
    }

    public StaffProfile(UserAccount userAccount, String fullName, String phone, String staffType, String specialty) {
        this(UUID.randomUUID(), userAccount, fullName, phone, staffType, specialty, true, Instant.now());
    }

    public StaffProfile(UUID id, UserAccount userAccount, String fullName, String phone,
                        String staffType, String specialty, boolean active, Instant createdAt) {
        super(id, userAccount, fullName, phone, createdAt);
        this.staffType = validateStaffType(staffType);
        this.specialty = validateSpecialty(this.staffType, specialty);
        this.active = active;
    }

    public static String validateStaffType(String staffType) {
        if (staffType == null || staffType.isBlank()) {
            throw new IllegalArgumentException("staffType must not be null or blank");
        }
        return staffType.strip();
    }

    public static String validateSpecialty(String staffType, String specialty) {
        if (specialty == null || specialty.isBlank()) {
            return null;
        }
        return specialty.strip();
    }

    public String getStaffType() {
        return staffType;
    }

    public void setStaffType(String staffType) {
        this.staffType = validateStaffType(staffType);
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = validateSpecialty(this.staffType, specialty);
    }

    public void changeSpecialty(String newSpecialty) {
        this.specialty = validateSpecialty(this.staffType, newSpecialty);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    @Override
    public String toString() {
        return "StaffProfile{" +
                "id=" + getId() +
                ", fullName='" + getFullName() + '\'' +
                ", phone='" + getPhone() + '\'' +
                ", staffType='" + staffType + '\'' +
                ", specialty='" + specialty + '\'' +
                ", active=" + active +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}
