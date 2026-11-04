package com.example.clinic.scheduling.domain;

import com.example.clinic.booking.domain.AppointmentStatus;
import com.example.clinic.common.domain.TimeInterval;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NoOverlapConstraintTest {

    private final NoOverlapConstraint constraint = new NoOverlapConstraint();

    private final UUID staffId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private final UUID serviceId = UUID.randomUUID();

    private final Instant t0900 = Instant.parse("2026-11-04T09:00:00Z");
    private final Instant t0930 = Instant.parse("2026-11-04T09:30:00Z");
    private final Instant t0945 = Instant.parse("2026-11-04T09:45:00Z");
    private final Instant t1000 = Instant.parse("2026-11-04T10:00:00Z");

    private BookingValidationContext.Builder baseContext(TimeInterval interval) {
        return BookingValidationContext.builder()
                .staffId(staffId)
                .branchId(branchId)
                .serviceId(serviceId)
                .requestedInterval(interval);
    }

    @Test
    @DisplayName("Negative: Overlapping PENDING appointment blocks the slot with STAFF_OVERLAP")
    void shouldBlockWhenOverlappingWithPendingAppointment() {
        TimeInterval existingInterval = TimeInterval.of(t0900, t0930);
        TimeInterval candidateInterval = TimeInterval.of(t0900, t0930); // exact same

        BookingValidationContext context = baseContext(candidateInterval)
                .addExistingAppointment(UUID.randomUUID(), staffId, existingInterval, AppointmentStatus.PENDING)
                .build();

        assertThatThrownBy(() -> constraint.validate(context))
                .isInstanceOf(SchedulingConflictException.class)
                .extracting("errorCode")
                .isEqualTo(SchedulingConflictException.ErrorCode.STAFF_OVERLAP);
    }

    @Test
    @DisplayName("Negative: Overlapping CONFIRMED appointment blocks the slot with STAFF_OVERLAP")
    void shouldBlockWhenOverlappingWithConfirmedAppointment() {
        TimeInterval existingInterval = TimeInterval.of(t0900, t0945);
        TimeInterval candidateInterval = TimeInterval.of(t0930, t1000); // partial overlap 09:30-09:45

        BookingValidationContext context = baseContext(candidateInterval)
                .addExistingAppointment(UUID.randomUUID(), staffId, existingInterval, AppointmentStatus.CONFIRMED)
                .build();

        assertThatThrownBy(() -> constraint.validate(context))
                .isInstanceOf(SchedulingConflictException.class)
                .extracting("errorCode")
                .isEqualTo(SchedulingConflictException.ErrorCode.STAFF_OVERLAP);
    }

    @Test
    @DisplayName("Negative: Overlapping COMPLETED past appointment blocks the slot with STAFF_OVERLAP")
    void shouldBlockWhenOverlappingWithCompletedAppointment() {
        TimeInterval existingInterval = TimeInterval.of(t0900, t0930);
        TimeInterval candidateInterval = TimeInterval.of(t0900, t0930);

        BookingValidationContext context = baseContext(candidateInterval)
                .addExistingAppointment(UUID.randomUUID(), staffId, existingInterval, AppointmentStatus.COMPLETED)
                .build();

        assertThatThrownBy(() -> constraint.validate(context))
                .isInstanceOf(SchedulingConflictException.class)
                .extracting("errorCode")
                .isEqualTo(SchedulingConflictException.ErrorCode.STAFF_OVERLAP);
    }

    @Test
    @DisplayName("Positive: Overlapping CANCELLED appointment releases the slot and does NOT block")
    void shouldNotBlockWhenExistingAppointmentIsCancelled() {
        TimeInterval existingInterval = TimeInterval.of(t0900, t0930);
        TimeInterval candidateInterval = TimeInterval.of(t0900, t0930);

        BookingValidationContext context = baseContext(candidateInterval)
                .addExistingAppointment(UUID.randomUUID(), staffId, existingInterval, AppointmentStatus.CANCELLED)
                .build();

        assertThatCode(() -> constraint.validate(context)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Positive: Overlapping REJECTED appointment releases the slot and does NOT block")
    void shouldNotBlockWhenExistingAppointmentIsRejected() {
        TimeInterval existingInterval = TimeInterval.of(t0900, t0930);
        TimeInterval candidateInterval = TimeInterval.of(t0900, t0930);

        BookingValidationContext context = baseContext(candidateInterval)
                .addExistingAppointment(UUID.randomUUID(), staffId, existingInterval, AppointmentStatus.REJECTED)
                .build();

        assertThatCode(() -> constraint.validate(context)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Positive: Adjacent interval for the same staff member does NOT overlap")
    void shouldAllowAdjacentAppointmentsForSameStaff() {
        TimeInterval existingInterval = TimeInterval.of(t0900, t0930);
        TimeInterval candidateInterval = TimeInterval.of(t0930, t1000); // adjacent [09:00, 09:30) & [09:30, 10:00)

        BookingValidationContext context = baseContext(candidateInterval)
                .addExistingAppointment(UUID.randomUUID(), staffId, existingInterval, AppointmentStatus.CONFIRMED)
                .build();

        assertThatCode(() -> constraint.validate(context)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Positive: Overlapping appointment belonging to a different staff member does NOT block")
    void shouldAllowSameIntervalForDifferentStaffMember() {
        UUID otherStaffId = UUID.randomUUID();
        TimeInterval interval = TimeInterval.of(t0900, t0930);

        BookingValidationContext context = baseContext(interval)
                .addExistingAppointment(UUID.randomUUID(), otherStaffId, interval, AppointmentStatus.CONFIRMED)
                .build();

        assertThatCode(() -> constraint.validate(context)).doesNotThrowAnyException();
    }
}
