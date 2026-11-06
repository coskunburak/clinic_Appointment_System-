package com.example.clinic.identity.domain;

import com.example.clinic.common.domain.BaseEntity;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "membership")
public class Membership extends BaseEntity {

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

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private Role role;

    protected Membership() {
        super();
    }

    public Membership(UserAccount userAccount, Organization organization, Branch branch, Role role) {
        this(UUID.randomUUID(), userAccount, organization, branch, role, Instant.now());
    }

    public Membership(UserAccount userAccount, Organization organization, Branch branch, String role) {
        this(UUID.randomUUID(), userAccount, organization, branch, Role.from(role), Instant.now());
    }

    public Membership(UUID id, UserAccount userAccount, Organization organization, Branch branch, Role role, Instant createdAt) {
        super(id, createdAt);
        this.userAccount = Objects.requireNonNull(userAccount, "UserAccount must not be null");
        this.organization = Objects.requireNonNull(organization, "Organization must not be null");
        this.branch = branch;
        this.role = Objects.requireNonNull(role, "role must not be null");
    }

    public Membership(UUID id, UserAccount userAccount, Organization organization, Branch branch, String role, Instant createdAt) {
        this(id, userAccount, organization, branch, Role.from(role), createdAt);
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = Objects.requireNonNull(role, "role must not be null");
    }

    public void setRole(String role) {
        this.role = Role.from(role);
    }

    @Override
    public String toString() {
        return "Membership{" +
                "id=" + getId() +
                ", role=" + role +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}
