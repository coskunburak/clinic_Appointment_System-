package com.example.clinic.scheduling.domain;

import com.example.clinic.common.domain.TimeInterval;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpeningHoursConstraintTest {

    private final OpeningHoursConstraint constraint = new OpeningHoursConstraint();
    private final ZoneId istanbulZone = ZoneId.of("Europe/Istanbul");

    private BookingValidationContext.Builder baseContext(TimeInterval interval) {
        return BookingValidationContext.builder()
                .staffId(UUID.randomUUID())
                .branchId(UUID.randomUUID())
                .serviceId(UUID.randomUUID())
                .branchTimeZone(istanbulZone)
                .requestedInterval(interval);
    }

    @Test
    @DisplayName("Positive: Appointment fully within branch opening hours succeeds")
    void shouldPassWhenIntervalIsWithinOpeningHours() {
        // Wednesday Nov 4, 2026: 10:00 to 10:30 Istanbul time (UTC: 07:00 to 07:30)
        ZonedDateTime startZoned = ZonedDateTime.of(2026, 11, 4, 10, 0, 0, 0, istanbulZone);
        TimeInterval interval = TimeInterval.of(startZoned.toInstant(), startZoned.plusMinutes(30).toInstant());

        BookingValidationContext context = baseContext(interval)
                .addOpeningWindow(DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(17, 0))
                .build();

        assertThatCode(() -> constraint.validate(context)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Boundary: Appointment exact match with opening and closing boundaries succeeds")
    void shouldPassWhenExactMatchWithOpeningAndClosingTimes() {
        ZonedDateTime startZoned = ZonedDateTime.of(2026, 11, 4, 9, 0, 0, 0, istanbulZone);
        ZonedDateTime endZoned = ZonedDateTime.of(2026, 11, 4, 17, 0, 0, 0, istanbulZone);
        TimeInterval interval = TimeInterval.of(startZoned.toInstant(), endZoned.toInstant());

        BookingValidationContext context = baseContext(interval)
                .addOpeningWindow(DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(17, 0))
                .build();

        assertThatCode(() -> constraint.validate(context)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Negative: Appointment starting before branch opens is rejected with BRANCH_CLOSED")
    void shouldRejectWhenStartingBeforeOpeningTime() {
        ZonedDateTime startZoned = ZonedDateTime.of(2026, 11, 4, 8, 45, 0, 0, istanbulZone); // opens at 9:00
        TimeInterval interval = TimeInterval.of(startZoned.toInstant(), startZoned.plusMinutes(30).toInstant());

        BookingValidationContext context = baseContext(interval)
                .addOpeningWindow(DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(17, 0))
                .build();

        assertThatThrownBy(() -> constraint.validate(context))
                .isInstanceOf(SchedulingConflictException.class)
                .extracting("errorCode")
                .isEqualTo(SchedulingConflictException.ErrorCode.BRANCH_CLOSED);
    }

    @Test
    @DisplayName("Negative: Appointment ending after branch closes is rejected with BRANCH_CLOSED")
    void shouldRejectWhenEndingAfterClosingTime() {
        ZonedDateTime startZoned = ZonedDateTime.of(2026, 11, 4, 16, 45, 0, 0, istanbulZone); // closes at 17:00, ends 17:15
        TimeInterval interval = TimeInterval.of(startZoned.toInstant(), startZoned.plusMinutes(30).toInstant());

        BookingValidationContext context = baseContext(interval)
                .addOpeningWindow(DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(17, 0))
                .build();

        assertThatThrownBy(() -> constraint.validate(context))
                .isInstanceOf(SchedulingConflictException.class)
                .extracting("errorCode")
                .isEqualTo(SchedulingConflictException.ErrorCode.BRANCH_CLOSED);
    }

    @Test
    @DisplayName("Negative: Appointment on a day when the branch is closed is rejected with BRANCH_CLOSED")
    void shouldRejectWhenBranchClosedOnGivenDay() {
        // Sunday Nov 8, 2026
        ZonedDateTime startZoned = ZonedDateTime.of(2026, 11, 8, 11, 0, 0, 0, istanbulZone);
        TimeInterval interval = TimeInterval.of(startZoned.toInstant(), startZoned.plusMinutes(30).toInstant());

        // Branch only open Mon-Fri
        BookingValidationContext context = baseContext(interval)
                .addOpeningWindow(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0))
                .addOpeningWindow(DayOfWeek.FRIDAY, LocalTime.of(9, 0), LocalTime.of(17, 0))
                .build();

        assertThatThrownBy(() -> constraint.validate(context))
                .isInstanceOf(SchedulingConflictException.class)
                .extracting("errorCode")
                .isEqualTo(SchedulingConflictException.ErrorCode.BRANCH_CLOSED);
    }

    @Test
    @DisplayName("Negative: Cross-midnight interval is rejected with BRANCH_CLOSED")
    void shouldRejectCrossMidnightInterval() {
        ZonedDateTime startZoned = ZonedDateTime.of(2026, 11, 4, 23, 30, 0, 0, istanbulZone);
        ZonedDateTime endZoned = ZonedDateTime.of(2026, 11, 5, 0, 30, 0, 0, istanbulZone);
        TimeInterval interval = TimeInterval.of(startZoned.toInstant(), endZoned.toInstant());

        BookingValidationContext context = baseContext(interval)
                .addOpeningWindow(DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(23, 59))
                .build();

        assertThatThrownBy(() -> constraint.validate(context))
                .isInstanceOf(SchedulingConflictException.class)
                .extracting("errorCode")
                .isEqualTo(SchedulingConflictException.ErrorCode.BRANCH_CLOSED);
    }
}
