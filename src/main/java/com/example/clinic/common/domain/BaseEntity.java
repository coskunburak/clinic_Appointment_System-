package com.example.clinic.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Shared mapped superclass providing consistent identity and creation audit state.
 * <p>
 * Architectural Boundary Note:
 * This class resides in the persistence-backed entity hierarchy and carries Jakarta Persistence
 * mappings to preserve compatibility with existing PostgreSQL V1 tables. Pure domain models
 * (such as Appointment in S02) remain completely independent of persistence annotations.
 * <p>
 * Schema Compatibility:
 * Applied exclusively to entities whose PostgreSQL V1 schema defines both {@code id UUID} and
 * {@code created_at TIMESTAMPTZ}. Entities lacking {@code created_at} (e.g. {@code BranchOpeningHours})
 * deliberately do not extend this class to prevent invalid schema validation failures.
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BaseEntity() {
        // Required by JPA specification
    }

    protected BaseEntity(UUID id, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "Entity id must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Entity createdAt must not be null");
    }

    protected BaseEntity(UUID id) {
        this(id, Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Enforces UUID-based equality scoped strictly to matching concrete entity classes.
     * Two unsaved entities with null IDs are never equal.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return id != null && id.equals(that.id);
    }

    /**
     * Returns a stable hash code throughout entity state transitions (transient, persisted, detached),
     * ensuring deterministic membership in hash-based collections.
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
