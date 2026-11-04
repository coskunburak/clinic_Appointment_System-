package com.example.clinic.scheduling.domain;

import com.example.clinic.booking.domain.AppointmentStatus;
import com.example.clinic.common.domain.TimeInterval;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SchedulingConstraintTest {

    private final ZoneId istanbulZone = ZoneId.of("Europe/Istanbul");

    /**
     * Demonstrates the Open/Closed Principle (OCP):
     * A novel holiday constraint is added to the scheduling engine without modifying
     * SchedulingValidator or any of the existing four constraints.
     */
    private static class OfficialHolidayConstraint implements SchedulingConstraint {
        private final ZonedDateTime holidayStart;
        private final ZonedDateTime holidayEnd;

        OfficialHolidayConstraint(ZonedDateTime holidayStart, ZonedDateTime holidayEnd) {
            this.holidayStart = holidayStart;
            this.holidayEnd = holidayEnd;
        }

        @Override
        public void validate(BookingValidationContext context) {
            TimeInterval holidayInterval = TimeInterval.of(holidayStart.toInstant(), holidayEnd.toInstant());
            if (holidayInterval.overlapsWith(context.requestedInterval())) {
                throw new SchedulingConflictException(
                        SchedulingConflictException.ErrorCode.BRANCH_CLOSED,
                        "Clinic is closed due to official holiday"
                );
            }
        }
    }

    @Test
    @DisplayName("Polymorphism: All four strategies plus custom strategy run through SchedulingConstraint interface")
    void shouldExecuteAllStrategiesPolymorphically() {
        UUID staffId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        // Wednesday Nov 4, 2026: 10:00 - 10:30 Istanbul
        ZonedDateTime startZoned = ZonedDateTime.of(2026, 11, 4, 10, 0, 0, 0, istanbulZone);
        TimeInterval interval = TimeInterval.of(startZoned.toInstant(), startZoned.plusMinutes(30).toInstant());

        BookingValidationContext context = BookingValidationContext.builder()
                .staffId(staffId)
                .branchId(branchId)
                .serviceId(serviceId)
                .branchTimeZone(istanbulZone)
                .requestedInterval(interval)
                // 1. Branch open 09:00 - 17:00
                .addOpeningWindow(DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(17, 0))
                // 2. Staff available 09:00 - 13:00
                .addStaffWorkingWindow(staffId, DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(13, 0))
                // 3. Staff eligible
                .addEligibility(staffId, branchId, serviceId)
                // 4. Non-conflicting adjacent appointment (09:30 - 10:00)
                .addExistingAppointment(UUID.randomUUID(), staffId,
                        TimeInterval.of(startZoned.minusMinutes(30).toInstant(), startZoned.toInstant()),
                        AppointmentStatus.CONFIRMED)
                .build();

        // Instantiate all 4 concrete strategies
        List<SchedulingConstraint> policies = List.of(
                new OpeningHoursConstraint(),
                new StaffAvailabilityConstraint(),
                new StaffServiceEligibilityConstraint(),
                new NoOverlapConstraint(),
                // 5. Open/Closed demonstration: Custom constraint participation
                new OfficialHolidayConstraint(
                        ZonedDateTime.of(2026, 10, 29, 0, 0, 0, 0, istanbulZone),
                        ZonedDateTime.of(2026, 10, 29, 23, 59, 59, 0, istanbulZone)
                )
        );

        SchedulingValidator validator = new SchedulingValidator(policies);

        // All 5 strategies pass polymorphically
        assertThatCode(() -> validator.validate(context)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Open/Closed: Novel strategy rejects booking without altering coordinator")
    void shouldRejectViaNovelConstraintUnderOpenClosedPrinciple() {
        UUID staffId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        ZonedDateTime startZoned = ZonedDateTime.of(2026, 10, 29, 10, 0, 0, 0, istanbulZone); // Republic Day holiday
        TimeInterval interval = TimeInterval.of(startZoned.toInstant(), startZoned.plusMinutes(30).toInstant());

        BookingValidationContext context = BookingValidationContext.builder()
                .staffId(staffId)
                .branchId(branchId)
                .serviceId(serviceId)
                .branchTimeZone(istanbulZone)
                .requestedInterval(interval)
                .addOpeningWindow(DayOfWeek.THURSDAY, LocalTime.of(9, 0), LocalTime.of(17, 0))
                .addStaffWorkingWindow(staffId, DayOfWeek.THURSDAY, LocalTime.of(9, 0), LocalTime.of(17, 0))
                .addEligibility(staffId, branchId, serviceId)
                .build();

        SchedulingValidator validator = new SchedulingValidator(List.of(
                new OpeningHoursConstraint(),
                new OfficialHolidayConstraint(
                        ZonedDateTime.of(2026, 10, 29, 0, 0, 0, 0, istanbulZone),
                        ZonedDateTime.of(2026, 10, 29, 23, 59, 59, 0, istanbulZone)
                )
        ));

        assertThatThrownBy(() -> validator.validate(context))
                .isInstanceOf(SchedulingConflictException.class)
                .hasMessageContaining("official holiday");
    }
}
