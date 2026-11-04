package com.example.clinic.organization.domain;

import com.example.clinic.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "organization")
public class Organization extends BaseEntity {

    @NotBlank
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Organization() {
        super();
    }

    public Organization(String name) {
        this(UUID.randomUUID(), name, true, Instant.now());
    }

    public Organization(UUID id, String name, boolean active, Instant createdAt) {
        super(id, createdAt);
        this.name = Objects.requireNonNull(name, "Organization name must not be null");
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "Organization name must not be null");
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
        return "Organization{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", active=" + active +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}
