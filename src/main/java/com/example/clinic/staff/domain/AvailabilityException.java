package com.example.clinic.staff.domain;

import com.example.clinic.common.domain.BaseEntity;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing date-specific schedule modifications such as leaves or overtime.
 * Fulfills FR-011, SCH-03, and TC-011.
 */
@Entity
@Table(name = "availability_exception")
public class AvailabilityException extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private StaffAssignment assignment;

    @NotNull
    @Column(name = "local_date", nullable = false)
    private LocalDate localDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 50)
    private AvailabilityExceptionKind kind;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "reason", length = 255)
    private String reason;

    protected AvailabilityException() {
        super();
    }

    public AvailabilityException(StaffAssignment assignment, LocalDate localDate, AvailabilityExceptionKind kind,
                                 LocalTime startTime, LocalTime endTime, String reason) {
        this(UUID.randomUUID(), assignment, localDate, kind, startTime, endTime, reason, Instant.now());
    }

    public AvailabilityException(UUID id, StaffAssignment assignment, LocalDate localDate,
                                 AvailabilityExceptionKind kind, LocalTime startTime, LocalTime endTime,
                                 String reason, Instant createdAt) {
        super(id, createdAt);
        this.assignment = Objects.requireNonNull(assignment, "assignment must not be null");
        this.localDate = Objects.requireNonNull(localDate, "localDate must not be null");
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
        validateTimesForKind(this.kind, startTime, endTime);
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason != null && !reason.isBlank() ? reason.strip() : null;
    }

    public static AvailabilityException createFullDayLeave(StaffAssignment assignment, LocalDate localDate, String reason) {
        return new AvailabilityException(assignment, localDate, AvailabilityExceptionKind.FULL_DAY_LEAVE, null, null, reason);
    }

    public static AvailabilityException createPartialLeave(StaffAssignment assignment, LocalDate localDate,
                                                           LocalTime startTime, LocalTime endTime, String reason) {
        return new AvailabilityException(assignment, localDate, AvailabilityExceptionKind.PARTIAL_LEAVE, startTime, endTime, reason);
    }

    public static AvailabilityException createOvertime(StaffAssignment assignment, LocalDate localDate,
                                                       LocalTime startTime, LocalTime endTime, String reason) {
        return new AvailabilityException(assignment, localDate, AvailabilityExceptionKind.OVERTIME, startTime, endTime, reason);
    }

    public static void validateTimesForKind(AvailabilityExceptionKind kind, LocalTime startTime, LocalTime endTime) {
        if (kind == AvailabilityExceptionKind.FULL_DAY_LEAVE) {
            if (startTime != null || endTime != null) {
                throw new IllegalArgumentException("Full-day leave must not specify start or end times");
            }
        } else {
            if (startTime == null || endTime == null) {
                throw new IllegalArgumentException(kind + " requires both startTime and endTime");
            }
            if (!startTime.isBefore(endTime)) {
                throw new IllegalArgumentException("startTime (" + startTime + ") must be before endTime (" + endTime + ")");
            }
        }
    }

    public StaffAssignment getAssignment() {
        return assignment;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public AvailabilityExceptionKind getKind() {
        return kind;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getReason() {
        return reason;
    }

    public boolean isFullDayLeave() {
        return kind == AvailabilityExceptionKind.FULL_DAY_LEAVE;
    }

    public boolean isPartialLeave() {
        return kind == AvailabilityExceptionKind.PARTIAL_LEAVE;
    }

    public boolean isOvertime() {
        return kind == AvailabilityExceptionKind.OVERTIME;
    }

    @Override
    public String toString() {
        return "AvailabilityException{" +
                "id=" + getId() +
                ", assignmentId=" + (assignment != null ? assignment.getId() : null) +
                ", localDate=" + localDate +
                ", kind=" + kind +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", reason='" + reason + '\'' +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}
