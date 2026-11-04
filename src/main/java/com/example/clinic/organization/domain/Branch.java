package com.example.clinic.organization.domain;

import com.example.clinic.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "branch")
public class Branch extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @NotBlank
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank
    @Column(name = "time_zone", nullable = false, length = 50)
    private String timeZone = "Europe/Istanbul";

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Branch() {
        super();
    }

    public Branch(Organization organization, String name, String timeZone, String address) {
        this(UUID.randomUUID(), organization, name, timeZone, address, true, Instant.now());
    }

    public Branch(UUID id, Organization organization, String name, String timeZone, String address, boolean active, Instant createdAt) {
        super(id, createdAt);
        this.organization = Objects.requireNonNull(organization, "Organization must not be null");
        this.name = Objects.requireNonNull(name, "Branch name must not be null");
        this.timeZone = Objects.requireNonNull(timeZone, "timeZone must not be null");
        this.address = address;
        this.active = active;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = Objects.requireNonNull(organization, "Organization must not be null");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "Branch name must not be null");
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = Objects.requireNonNull(timeZone, "timeZone must not be null");
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "Branch{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", timeZone='" + timeZone + '\'' +
                ", active=" + active +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}
