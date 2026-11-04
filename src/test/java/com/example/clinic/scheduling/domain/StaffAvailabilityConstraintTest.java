package com.example.clinic.scheduling.domain;

import com.example.clinic.common.domain.TimeInterval;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StaffAvailabilityConstraintTest {

    private final StaffAvailabilityConstraint constraint = new StaffAvailabilityConstraint();
    private final ZoneId istanbulZone = ZoneId.of("Europe/Istanbul");

    @Test
    @DisplayName("Positive: Appointment fully covered by staff working window succeeds")
    void shouldPassWhenStaffIsAvailable() {
        UUID staffId = UUID.randomUUID();
        ZonedDateTime startZoned = ZonedDateTime.of(2026, 11, 4, 10, 0, 0, 0, istanbulZone);
        TimeInterval interval = TimeInterval.of(startZoned.toInstant(), startZoned.plusMinutes(30).toInstant());

        BookingValidationContext context = BookingValidationContext.builder()
                .staffId(staffId)
                .branchId(UUID.randomUUID())
                .serviceId(UUID.randomUUID())
                .branchTimeZone(istanbulZone)
                .requestedInterval(interval)
                .addStaffWorkingWindow(staffId, DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(13, 0))
                .build();

        assertThatCode(() -> constraint.validate(context)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Boundary: Appointment exactly matching staff shift boundaries succeeds")
    void shouldPassOnExactShiftBoundaryMatch() {
        UUID staffId = UUID.randomUUID();
        ZonedDateTime startZoned = ZonedDateTime.of(2026, 11, 4, 9, 0, 0, 0, istanbulZone);
        ZonedDateTime endZoned = ZonedDateTime.of(2026, 11, 4, 13, 0, 0, 0, istanbulZone);
        TimeInterval interval = TimeInterval.of(startZoned.toInstant(), endZoned.toInstant());

        BookingValidationContext context = BookingValidationContext.builder()
                .staffId(staffId)
                .branchId(UUID.randomUUID())
                .serviceId(UUID.randomUUID())
                .branchTimeZone(istanbulZone)
                .requestedInterval(interval)
                .addStaffWorkingWindow(staffId, DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(13, 0))
                .build();

        assertThatCode(() -> constraint.validate(context)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Negative: Partial overlap where appointment starts before staff shift is rejected with STAFF_UNAVAILABLE")
    void shouldRejectWhenStartingBeforeStaffShift() {
        UUID staffId = UUID.randomUUID();
        // Shift starts at 10:00, appointment requested at 09:45
        ZonedDateTime startZoned = ZonedDateTime.of(2026, 11, 4, 9, 45, 0, 0, istanbulZone);
        TimeInterval interval = TimeInterval.of(startZoned.toInstant(), startZoned.plusMinutes(30).toInstant());

        BookingValidationContext context = BookingValidationContext.builder()
                .staffId(staffId)
                .branchId(UUID.randomUUID())
                .serviceId(UUID.randomUUID())
                .branchTimeZone(istanbulZone)
                .requestedInterval(interval)
                .addStaffWorkingWindow(staffId, DayOfWeek.WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(14, 0))
                .build();

        assertThatThrownBy(() -> constraint.validate(context))
                .isInstanceOf(SchedulingConflictException.class)
                .extracting("errorCode")
                .isEqualTo(SchedulingConflictException.ErrorCode.STAFF_UNAVAILABLE);
    }

    @Test
    @DisplayName("Negative: Partial overlap where appointment ends after staff shift is rejected with STAFF_UNAVAILABLE")
    void shouldRejectWhenEndingAfterStaffShift() {
        UUID staffId = UUID.randomUUID();
        // Shift ends at 12:00, appointment runs 11:45 to 12:15
        ZonedDateTime startZoned = ZonedDateTime.of(2026, 11, 4, 11, 45, 0, 0, istanbulZone);
        TimeInterval interval = TimeInterval.of(startZoned.toInstant(), startZoned.plusMinutes(30).toInstant());

        BookingValidationContext context = BookingValidationContext.builder()
                .staffId(staffId)
                .branchId(UUID.randomUUID())
                .serviceId(UUID.randomUUID())
                .branchTimeZone(istanbulZone)
                .requestedInterval(interval)
                .addStaffWorkingWindow(staffId, DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(12, 0))
                .build();

        assertThatThrownBy(() -> constraint.validate(context))
                .isInstanceOf(SchedulingConflictException.class)
                .extracting("errorCode")
                .isEqualTo(SchedulingConflictException.ErrorCode.STAFF_UNAVAILABLE);
    }

    @Test
    @DisplayName("Negative: Working window of a different staff member does not satisfy availability")
    void shouldRejectWhenShiftBelongsToAnotherStaffMember() {
        UUID requestedStaffId = UUID.randomUUID();
        UUID otherStaffId = UUID.randomUUID();

        ZonedDateTime startZoned = ZonedDateTime.of(2026, 11, 4, 10, 0, 0, 0, istanbulZone);
        TimeInterval interval = TimeInterval.of(startZoned.toInstant(), startZoned.plusMinutes(30).toInstant());

        BookingValidationContext context = BookingValidationContext.builder()
                .staffId(requestedStaffId)
                .branchId(UUID.randomUUID())
                .serviceId(UUID.randomUUID())
                .branchTimeZone(istanbulZone)
                .requestedInterval(interval)
                .addStaffWorkingWindow(otherStaffId, DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(17, 0))
                .build();

        assertThatThrownBy(() -> constraint.validate(context))
                .isInstanceOf(SchedulingConflictException.class)
                .extracting("errorCode")
                .isEqualTo(SchedulingConflictException.ErrorCode.STAFF_UNAVAILABLE);
    }
}
