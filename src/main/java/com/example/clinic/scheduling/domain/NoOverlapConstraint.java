package com.example.clinic.scheduling.domain;

import java.util.Objects;

/**
 * Scheduling strategy that prevents overlapping appointments for the same staff member.
 * <p>
 * In accordance with ADR-003:
 * <ul>
 *   <li>PENDING, CONFIRMED, and COMPLETED appointments are blocking.</li>
 *   <li>CANCELLED and REJECTED appointments do not block the time interval.</li>
 *   <li>Adjacent intervals [09:00, 09:30) and [09:30, 10:00) do not overlap.</li>
 * </ul>
 */
public class NoOverlapConstraint implements SchedulingConstraint {

    @Override
    public void validate(BookingValidationContext context) {
        Objects.requireNonNull(context, "BookingValidationContext must not be null");

        for (BookingValidationContext.ExistingAppointmentFact existing : context.existingAppointments()) {
            // Only appointments assigned to the same staff member can conflict
            if (!existing.staffId().equals(context.staffId())) {
                continue;
            }

            // Cancelled and rejected appointments release the slot
            if (!existing.status().isBlocking()) {
                continue;
            }

            // Evaluate half-open interval overlap
            if (existing.interval().overlapsWith(context.requestedInterval())) {
                throw new SchedulingConflictException(
                        SchedulingConflictException.ErrorCode.STAFF_OVERLAP,
                        "Staff member " + context.staffId() + " already has a conflicting appointment (" +
                                existing.appointmentId() + ") in status " + existing.status() +
                                " during interval " + existing.interval()
                );
            }
        }
    }
}
