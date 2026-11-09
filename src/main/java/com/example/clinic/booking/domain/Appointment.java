package com.example.clinic.booking.domain;

import com.example.clinic.common.domain.TimeInterval;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Rich domain aggregate root representing a medical appointment.
 * <p>
 * Architectural Boundary Note:
 * In Sprint S02, this is a pure Java domain model completely independent of Spring and Jakarta
 * Persistence. Database schema and JPA repository persistence for Appointment are scheduled for S06.
 * <p>
 * Invariants & Encapsulation:
 * <ul>
 *   <li>Immutable identities (id, patientId, staffId, branchId, branchServiceId).</li>
 *   <li>Immutable interval and service snapshot; interval duration must exactly match service snapshot duration.</li>
 *   <li>State transitions are strictly governed by behavioral domain methods (approve, reject, cancel, complete).</li>
 *   <li>Arbitrary status mutation via public setters is prohibited.</li>
 *   <li>Time-dependent business rules are guarded deterministically via {@link Clock}.</li>
 * </ul>
 */
public class Appointment {

    private final UUID id;
    private final UUID patientId;
    private final UUID staffId;
    private final UUID branchId;
    private final UUID branchServiceId;
    private final TimeInterval interval;
    private final ServiceSnapshot serviceSnapshot;
    private AppointmentStatus status;
    private final Instant createdAt;
    private Long version;

    public Appointment(UUID id, UUID patientId, UUID staffId, UUID branchId, UUID branchServiceId,
                       TimeInterval interval, ServiceSnapshot serviceSnapshot, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "Appointment id must not be null");
        this.patientId = Objects.requireNonNull(patientId, "patientId must not be null");
        this.staffId = Objects.requireNonNull(staffId, "staffId must not be null");
        this.branchId = Objects.requireNonNull(branchId, "branchId must not be null");
        this.branchServiceId = Objects.requireNonNull(branchServiceId, "branchServiceId must not be null");
        this.interval = Objects.requireNonNull(interval, "interval must not be null");
        this.serviceSnapshot = Objects.requireNonNull(serviceSnapshot, "serviceSnapshot must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");

        long intervalMinutes = interval.duration().toMinutes();
        long serviceMinutes = serviceSnapshot.durationMinutes();
        if (intervalMinutes != serviceMinutes) {
            throw new IllegalArgumentException(
                    "Interval duration (" + intervalMinutes + " mins) does not match service snapshot duration (" +
                            serviceMinutes + " mins)"
            );
        }

        this.status = AppointmentStatus.PENDING;
    }

    public static Appointment create(UUID id, UUID patientId, UUID staffId, UUID branchId, UUID branchServiceId,
                                    TimeInterval interval, ServiceSnapshot serviceSnapshot, Instant createdAt) {
        return new Appointment(id, patientId, staffId, branchId, branchServiceId, interval, serviceSnapshot, createdAt);
    }

    public static Appointment create(UUID patientId, UUID staffId, UUID branchId, UUID branchServiceId,
                                    TimeInterval interval, ServiceSnapshot serviceSnapshot) {
        return new Appointment(UUID.randomUUID(), patientId, staffId, branchId, branchServiceId,
                interval, serviceSnapshot, Instant.now());
    }

    /**
     * Reconstitutes an existing appointment aggregate from persistence representation.
     */
    public static Appointment reconstitute(UUID id, UUID patientId, UUID staffId, UUID branchId, UUID branchServiceId,
                                           TimeInterval interval, ServiceSnapshot serviceSnapshot,
                                           AppointmentStatus status, Instant createdAt, Long version) {
        Appointment appointment = new Appointment(id, patientId, staffId, branchId, branchServiceId, interval, serviceSnapshot, createdAt);
        appointment.status = Objects.requireNonNull(status, "AppointmentStatus must not be null");
        appointment.version = version;
        return appointment;
    }

    /**
     * Approves a PENDING appointment, transitioning it to CONFIRMED.
     *
     * @param actorId the UUID of the approving staff/manager
     * @param clock   the system or test clock
     * @throws AppointmentDomainException if status is not PENDING or if the appointment start time has passed
     */
    public void approve(UUID actorId, Clock clock) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(clock, "Clock must not be null");

        if (this.status != AppointmentStatus.PENDING) {
            throw new AppointmentDomainException(
                    "INVALID_STATUS_TRANSITION",
                    "Cannot approve appointment in status: " + this.status + " (must be PENDING)"
            );
        }

        Instant now = clock.instant();
        if (!now.isBefore(interval.startAt())) {
            throw new AppointmentDomainException(
                    "APPOINTMENT_ALREADY_STARTED",
                    "Cannot approve an appointment that has already started or passed (now=" + now +
                            ", startAt=" + interval.startAt() + ")"
            );
        }

        this.status = AppointmentStatus.CONFIRMED;
    }

    /**
     * Rejects a PENDING appointment, transitioning it to REJECTED.
     *
     * @param actorId the UUID of the rejecting staff/manager
     * @param reason  optional reason for rejection
     * @param clock   the system or test clock
     * @throws AppointmentDomainException if status is not PENDING
     */
    public void reject(UUID actorId, String reason, Clock clock) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(clock, "Clock must not be null");

        if (this.status != AppointmentStatus.PENDING) {
            throw new AppointmentDomainException(
                    "INVALID_STATUS_TRANSITION",
                    "Cannot reject appointment in status: " + this.status + " (must be PENDING)"
            );
        }

        this.status = AppointmentStatus.REJECTED;
    }

    /**
     * Cancels an appointment from either PENDING or CONFIRMED status.
     *
     * @param actorId the UUID of the patient or staff cancelling
     * @param reason  mandatory explanation for the cancellation
     * @param clock   the system or test clock
     * @throws AppointmentDomainException if status is not cancellable or appointment has already started
     */
    public void cancel(UUID actorId, String reason, Clock clock) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(clock, "Clock must not be null");

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Cancellation reason must not be null or blank");
        }

        if (this.status != AppointmentStatus.PENDING && this.status != AppointmentStatus.CONFIRMED) {
            throw new AppointmentDomainException(
                    "INVALID_STATUS_TRANSITION",
                    "Cannot cancel appointment in status: " + this.status + " (must be PENDING or CONFIRMED)"
            );
        }

        Instant now = clock.instant();
        if (!now.isBefore(interval.startAt())) {
            throw new AppointmentDomainException(
                    "APPOINTMENT_ALREADY_STARTED",
                    "Cannot cancel an appointment that has already started (now=" + now +
                            ", startAt=" + interval.startAt() + ")"
            );
        }

        this.status = AppointmentStatus.CANCELLED;
    }

    /**
     * Completes a CONFIRMED appointment once its scheduled end time has been reached.
     *
     * @param actorId the UUID of the attending staff/manager
     * @param clock   the system or test clock
     * @throws AppointmentDomainException if status is not CONFIRMED or if endAt has not yet been reached
     */
    public void complete(UUID actorId, Clock clock) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(clock, "Clock must not be null");

        if (this.status != AppointmentStatus.CONFIRMED) {
            throw new AppointmentDomainException(
                    "INVALID_STATUS_TRANSITION",
                    "Cannot complete appointment in status: " + this.status + " (must be CONFIRMED)"
            );
        }

        Instant now = clock.instant();
        if (now.isBefore(interval.endAt())) {
            throw new AppointmentDomainException(
                    "APPOINTMENT_NOT_ENDED",
                    "Cannot complete appointment before its scheduled end time (now=" + now +
                            ", endAt=" + interval.endAt() + ")"
            );
        }

        this.status = AppointmentStatus.COMPLETED;
    }

    public UUID getId() {
        return id;
    }

    public UUID id() {
        return id;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public UUID patientId() {
        return patientId;
    }

    public UUID getStaffId() {
        return staffId;
    }

    public UUID staffId() {
        return staffId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public UUID branchId() {
        return branchId;
    }

    public UUID getBranchServiceId() {
        return branchServiceId;
    }

    public UUID branchServiceId() {
        return branchServiceId;
    }

    public TimeInterval getInterval() {
        return interval;
    }

    public TimeInterval interval() {
        return interval;
    }

    public ServiceSnapshot getServiceSnapshot() {
        return serviceSnapshot;
    }

    public ServiceSnapshot serviceSnapshot() {
        return serviceSnapshot;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public AppointmentStatus status() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Long getVersion() {
        return version;
    }

    public Long version() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Appointment that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Appointment{" +
                "id=" + id +
                ", patientId=" + patientId +
                ", staffId=" + staffId +
                ", branchId=" + branchId +
                ", interval=" + interval +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
