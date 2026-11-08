package com.example.clinic.staff.domain;

import com.example.clinic.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing recurring weekly working hours for an assigned staff member.
 * Fulfills FR-010, SCH-02, and TC-010.
 */
@Entity
@Table(name = "weekly_availability")
public class WeeklyAvailability extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private StaffAssignment assignment;

    @Column(name = "weekday", nullable = false)
    private short weekday;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    protected WeeklyAvailability() {
        super();
    }

    public WeeklyAvailability(StaffAssignment assignment, short weekday, LocalTime startTime, LocalTime endTime) {
        this(UUID.randomUUID(), assignment, weekday, startTime, endTime, Instant.now());
    }

    public WeeklyAvailability(StaffAssignment assignment, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this(UUID.randomUUID(), assignment, (short) Objects.requireNonNull(dayOfWeek, "dayOfWeek must not be null").getValue(),
                startTime, endTime, Instant.now());
    }

    public WeeklyAvailability(UUID id, StaffAssignment assignment, short weekday,
                              LocalTime startTime, LocalTime endTime, Instant createdAt) {
        super(id, createdAt);
        this.assignment = Objects.requireNonNull(assignment, "assignment must not be null");
        this.weekday = validateWeekday(weekday);
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        validateTimeOrder(this.startTime, this.endTime);
    }

    public static short validateWeekday(short weekday) {
        if (weekday < 1 || weekday > 7) {
            throw new IllegalArgumentException("weekday must be between 1 and 7 (was: " + weekday + ")");
        }
        return weekday;
    }

    public static void validateTimeOrder(LocalTime startTime, LocalTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException(
                    "startTime (" + startTime + ") must be before endTime (" + endTime + ")"
            );
        }
    }

    public StaffAssignment getAssignment() {
        return assignment;
    }

    public short getWeekday() {
        return weekday;
    }

    public DayOfWeek getDayOfWeek() {
        return DayOfWeek.of(weekday);
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void updateWorkingHours(LocalTime newStartTime, LocalTime newEndTime) {
        Objects.requireNonNull(newStartTime, "newStartTime must not be null");
        Objects.requireNonNull(newEndTime, "newEndTime must not be null");
        validateTimeOrder(newStartTime, newEndTime);
        this.startTime = newStartTime;
        this.endTime = newEndTime;
    }

    @Override
    public String toString() {
        return "WeeklyAvailability{" +
                "id=" + getId() +
                ", assignmentId=" + (assignment != null ? assignment.getId() : null) +
                ", weekday=" + weekday + " (" + getDayOfWeek() + ")" +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}
