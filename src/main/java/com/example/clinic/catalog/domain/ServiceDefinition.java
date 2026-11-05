package com.example.clinic.catalog.domain;

import com.example.clinic.common.domain.BaseEntity;
import com.example.clinic.organization.domain.Organization;
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

/**
 * Represents a medical service defined at organization scope (e.g., General Examination, Cardiology Consultation).
 * <p>
 * Invariants:
 * - Owning organization must exist and is immutable after creation.
 * - Service name must be non-blank and unique within the owning organization.
 * - Lifecycle state (active/inactive) is managed via explicit business methods.
 */
@Entity
@Table(name = "service_definition", uniqueConstraints = {
        @UniqueConstraint(name = "uq_service_definition_org_name", columnNames = {"organization_id", "name"}),
        @UniqueConstraint(name = "uq_service_definition_id_org_id", columnNames = {"id", "organization_id"})
})
public class ServiceDefinition extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @NotBlank
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected ServiceDefinition() {
        super();
    }

    public ServiceDefinition(Organization organization, String name, String description) {
        this(UUID.randomUUID(), organization, name, description, true, Instant.now());
    }

    public ServiceDefinition(UUID id, Organization organization, String name, String description, boolean active, Instant createdAt) {
        super(id, createdAt);
        this.organization = Objects.requireNonNull(organization, "Organization must not be null");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Service definition name must not be blank");
        }
        this.name = name.strip();
        this.description = description != null ? description.strip() : null;
        this.active = active;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getName() {
        return name;
    }

    public void rename(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("New service definition name must not be blank");
        }
        this.name = newName.strip();
    }

    public String getDescription() {
        return description;
    }

    public void changeDescription(String newDescription) {
        this.description = newDescription != null ? newDescription.strip() : null;
    }

    public boolean isActive() {
        return active;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    @Override
    public String toString() {
        return "ServiceDefinition{" +
                "id=" + getId() +
                ", organizationId=" + (organization != null ? organization.getId() : null) +
                ", name='" + name + '\'' +
                ", active=" + active +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}
