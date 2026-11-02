package com.example.clinic.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "staff_profile")
public class StaffProfile {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserAccount userAccount;

    @NotBlank
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "phone", length = 50)
    private String phone;

    @NotBlank
    @Column(name = "staff_type", nullable = false, length = 50)
    private String staffType;

    @Column(name = "specialty", length = 100)
    private String specialty;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StaffProfile() {
    }

    public StaffProfile(UserAccount userAccount, String fullName, String phone, String staffType, String specialty) {
        this(UUID.randomUUID(), userAccount, fullName, phone, staffType, specialty, true, Instant.now());
    }

    public StaffProfile(UUID id, UserAccount userAccount, String fullName, String phone, String staffType, String specialty, boolean active, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "StaffProfile id must not be null");
        this.userAccount = Objects.requireNonNull(userAccount, "UserAccount must not be null");
        this.fullName = Objects.requireNonNull(fullName, "fullName must not be null");
        this.phone = phone;
        this.staffType = Objects.requireNonNull(staffType, "staffType must not be null");
        this.specialty = specialty;
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public UUID getId() {
        return id;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(UserAccount userAccount) {
        this.userAccount = Objects.requireNonNull(userAccount, "userAccount must not be null");
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStaffType() {
        return staffType;
    }

    public void setStaffType(String staffType) {
        this.staffType = Objects.requireNonNull(staffType, "staffType must not be null");
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StaffProfile that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "StaffProfile{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", staffType='" + staffType + '\'' +
                ", specialty='" + specialty + '\'' +
                ", active=" + active +
                ", createdAt=" + createdAt +
                '}';
    }
}
