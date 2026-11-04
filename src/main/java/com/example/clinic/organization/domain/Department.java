package com.example.clinic.organization.domain;

import com.example.clinic.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "department", uniqueConstraints = {
        @UniqueConstraint(name = "uq_department_branch_name", columnNames = {"branch_id", "name"})
})
public class Department extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @NotBlank
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Department() {
        super();
    }

    public Department(Branch branch, String name) {
        this(UUID.randomUUID(), branch, name, true, Instant.now());
    }

    public Department(UUID id, Branch branch, String name, boolean active, Instant createdAt) {
        super(id, createdAt);
        this.branch = Objects.requireNonNull(branch, "Branch must not be null");
        this.name = Objects.requireNonNull(name, "Department name must not be null");
        this.active = active;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = Objects.requireNonNull(branch, "Branch must not be null");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "Department name must not be null");
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "Department{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", active=" + active +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}
