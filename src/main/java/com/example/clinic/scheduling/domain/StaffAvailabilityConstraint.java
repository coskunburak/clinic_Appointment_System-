package com.example.clinic.scheduling.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Scheduling strategy that validates whether the assigned staff member has an active working
 * window covering the entire requested appointment interval.
 */
public class StaffAvailabilityConstraint implements SchedulingConstraint {

    @Override
    public void validate(BookingValidationContext context) {
        Objects.requireNonNull(context, "BookingValidationContext must not be null");

        ZonedDateTime startZoned = context.requestedInterval().startAt().atZone(context.branchTimeZone());
        ZonedDateTime endZoned = context.requestedInterval().endAt().atZone(context.branchTimeZone());

        if (!startZoned.toLocalDate().equals(endZoned.toLocalDate())) {
            throw new SchedulingConflictException(
                    SchedulingConflictException.ErrorCode.STAFF_UNAVAILABLE,
                    "Requested appointment interval spans across midnight or multiple days"
            );
        }

        DayOfWeek dayOfWeek = startZoned.getDayOfWeek();
        LocalTime reqStart = startZoned.toLocalTime();
        LocalTime reqEnd = endZoned.toLocalTime();

        boolean covered = context.staffWorkingWindows().stream()
                .filter(w -> w.staffId().equals(context.staffId()) && w.dayOfWeek() == dayOfWeek)
                .anyMatch(w -> !reqStart.isBefore(w.startTime()) && !reqEnd.isAfter(w.endTime()));

        if (!covered) {
            throw new SchedulingConflictException(
                    SchedulingConflictException.ErrorCode.STAFF_UNAVAILABLE,
                    "Staff member " + context.staffId() + " is not available during " +
                            reqStart + " - " + reqEnd + " on " + dayOfWeek
            );
        }
    }
}
