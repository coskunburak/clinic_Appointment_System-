package com.example.clinic.identity.domain;

import com.example.clinic.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Abstract mapped superclass encapsulating shared personal profile attributes and behavior.
 * <p>
 * Demonstrates:
 * <ul>
 *   <li>Inheritance: Shared identity, contact details and invariants for PatientProfile and StaffProfile.</li>
 *   <li>Liskov Substitution Principle (LSP): Subclasses can be treated uniformly through this abstraction.</li>
 *   <li>Encapsulation: Invariants enforced on name and telephone inputs.</li>
 * </ul>
 */
@MappedSuperclass
public abstract class PersonProfile extends BaseEntity {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9\\s\\-\\(\\)]{7,50}$");

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserAccount userAccount;

    @NotBlank
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "phone", length = 50)
    private String phone;

    protected PersonProfile() {
        super();
    }

    protected PersonProfile(UUID id, UserAccount userAccount, String fullName, String phone, Instant createdAt) {
        super(id, createdAt);
        this.userAccount = Objects.requireNonNull(userAccount, "UserAccount must not be null");
        this.fullName = validateFullName(fullName);
        this.phone = validatePhone(phone);
    }

    public static String validateFullName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Full name must not be null or blank");
        }
        return name.strip();
    }

    public static String validatePhone(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return null;
        }
        String stripped = rawPhone.strip();
        if (!PHONE_PATTERN.matcher(stripped).matches()) {
            throw new IllegalArgumentException("Invalid phone number format: " + rawPhone);
        }
        return stripped;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(UserAccount userAccount) {
        this.userAccount = Objects.requireNonNull(userAccount, "UserAccount must not be null");
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = validateFullName(fullName);
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = validatePhone(phone);
    }

    public void rename(String newFullName) {
        this.fullName = validateFullName(newFullName);
    }

    public void changePhone(String newPhone) {
        this.phone = validatePhone(newPhone);
    }

    public void changeContactInformation(String newFullName, String newPhone) {
        this.fullName = validateFullName(newFullName);
        this.phone = validatePhone(newPhone);
    }
}
