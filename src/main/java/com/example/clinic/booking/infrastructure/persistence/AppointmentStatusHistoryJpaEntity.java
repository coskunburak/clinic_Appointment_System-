package com.example.clinic.booking.infrastructure.persistence;

import com.example.clinic.booking.domain.AppointmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Append-only audit entity recording lifecycle status transitions for an appointment.
 * Fulfills TC-017 for creation event and establishes foundation for S07 transition logging.
 */
@Entity
@Table(name = "appointment_status_history")
public class AppointmentStatusHistoryJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "appointment_id", nullable = false, updatable = false)
    private UUID appointmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30, updatable = false)
    private AppointmentStatus fromStatus;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30, updatable = false)
    private AppointmentStatus toStatus;

    @NotNull
    @Column(name = "actor_user_id", nullable = false, updatable = false)
    private UUID actorUserId;

    @Column(name = "reason", length = 500, updatable = false)
    private String reason;

    @NotNull
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AppointmentStatusHistoryJpaEntity() {
    }

    public AppointmentStatusHistoryJpaEntity(UUID id, UUID appointmentId, AppointmentStatus fromStatus,
                                             AppointmentStatus toStatus, UUID actorUserId,
                                             String reason, Instant occurredAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.appointmentId = Objects.requireNonNull(appointmentId, "appointmentId must not be null");
        this.fromStatus = fromStatus; // Can be null for initial creation
        this.toStatus = Objects.requireNonNull(toStatus, "toStatus must not be null");
        this.actorUserId = Objects.requireNonNull(actorUserId, "actorUserId must not be null");
        this.reason = reason;
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public UUID getId() {
        return id;
    }

    public UUID getAppointmentId() {
        return appointmentId;
    }

    public AppointmentStatus getFromStatus() {
        return fromStatus;
    }

    public AppointmentStatus getToStatus() {
        return toStatus;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getReason() {
        return reason;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AppointmentStatusHistoryJpaEntity that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
