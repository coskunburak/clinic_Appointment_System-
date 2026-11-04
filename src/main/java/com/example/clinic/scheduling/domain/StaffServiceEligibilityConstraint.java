package com.example.clinic.scheduling.domain;

import java.util.Objects;

/**
 * Scheduling strategy that validates whether the assigned staff member is certified and eligible
 * to perform the selected medical service at the designated branch.
 */
public class StaffServiceEligibilityConstraint implements SchedulingConstraint {

    @Override
    public void validate(BookingValidationContext context) {
        Objects.requireNonNull(context, "BookingValidationContext must not be null");

        boolean eligible = context.staffServiceEligibilities().stream()
                .anyMatch(f -> f.staffId().equals(context.staffId()) &&
                        f.branchId().equals(context.branchId()) &&
                        f.serviceId().equals(context.serviceId()));

        if (!eligible) {
            throw new SchedulingConflictException(
                    SchedulingConflictException.ErrorCode.STAFF_NOT_ELIGIBLE,
                    "Staff member " + context.staffId() + " is not eligible to deliver service " +
                            context.serviceId() + " at branch " + context.branchId()
            );
        }
    }
}
