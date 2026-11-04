package com.example.clinic.scheduling.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Scheduling strategy that validates whether the requested appointment interval fits
 * entirely within the branch's published opening hours.
 */
public class OpeningHoursConstraint implements SchedulingConstraint {

    @Override
    public void validate(BookingValidationContext context) {
        Objects.requireNonNull(context, "BookingValidationContext must not be null");

        ZonedDateTime startZoned = context.requestedInterval().startAt().atZone(context.branchTimeZone());
        ZonedDateTime endZoned = context.requestedInterval().endAt().atZone(context.branchTimeZone());

        // Appointments cannot cross midnight into the next calendar day
        if (!startZoned.toLocalDate().equals(endZoned.toLocalDate())) {
            throw new SchedulingConflictException(
                    SchedulingConflictException.ErrorCode.BRANCH_CLOSED,
                    "Requested appointment interval spans across midnight in branch time zone (" +
                            context.branchTimeZone() + ")"
            );
        }

        DayOfWeek dayOfWeek = startZoned.getDayOfWeek();
        LocalTime reqStart = startZoned.toLocalTime();
        LocalTime reqEnd = endZoned.toLocalTime();

        boolean covered = context.branchOpeningWindows().stream()
                .filter(w -> w.dayOfWeek() == dayOfWeek)
                .anyMatch(w -> !reqStart.isBefore(w.openTime()) && !reqEnd.isAfter(w.closeTime()));

        if (!covered) {
            throw new SchedulingConflictException(
                    SchedulingConflictException.ErrorCode.BRANCH_CLOSED,
                    "Branch is closed during requested interval: " + reqStart + " - " + reqEnd +
                            " on " + dayOfWeek + " (" + context.branchTimeZone() + ")"
            );
        }
    }
}
