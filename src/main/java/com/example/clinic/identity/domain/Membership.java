package com.example.clinic.identity.domain;

import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "membership")
public class Membership {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount userAccount;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @NotBlank
    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Membership() {
    }

    public Membership(UserAccount userAccount, Organization organization, Branch branch, String role) {
        this(UUID.randomUUID(), userAccount, organization, branch, role, Instant.now());
    }

    public Membership(UUID id, UserAccount userAccount, Organization organization, Branch branch, String role, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "Membership id must not be null");
        this.userAccount = Objects.requireNonNull(userAccount, "UserAccount must not be null");
        this.organization = Objects.requireNonNull(organization, "Organization must not be null");
        this.branch = branch;
        this.role = Objects.requireNonNull(role, "Role must not be null");
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

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = Objects.requireNonNull(organization, "organization must not be null");
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = Objects.requireNonNull(role, "role must not be null");
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Membership that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Membership{" +
                "id=" + id +
                ", role='" + role + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
