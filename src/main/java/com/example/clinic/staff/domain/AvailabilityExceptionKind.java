package com.example.clinic.staff.domain;

import java.util.Locale;

/**
 * Kind of exception to routine staff availability (leave or overtime).
 * Fulfills FR-011 and SCH-03.
 */
public enum AvailabilityExceptionKind {
    FULL_DAY_LEAVE,
    PARTIAL_LEAVE,
    OVERTIME;

    public static AvailabilityExceptionKind fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("AvailabilityExceptionKind must not be null or blank");
        }
        try {
            return AvailabilityExceptionKind.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown AvailabilityExceptionKind: '" + value +
                    "'. Must be one of: FULL_DAY_LEAVE, PARTIAL_LEAVE, OVERTIME");
        }
    }

    public boolean isLeave() {
        return this == FULL_DAY_LEAVE || this == PARTIAL_LEAVE;
    }

    public boolean requiresTimeRange() {
        return this == PARTIAL_LEAVE || this == OVERTIME;
    }
}
