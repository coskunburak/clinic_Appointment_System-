package com.example.clinic.identity.domain;

import java.util.Locale;

/**
 * Type-safe enumeration of medical clinic staff roles fulfilling FR-007 and STA-01.
 */
public enum StaffType {
    DOCTOR,
    NURSE,
    TECHNICIAN;

    public static StaffType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("staffType must not be null or blank");
        }
        try {
            return StaffType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown staffType: '" + value + "'. Must be one of: DOCTOR, NURSE, TECHNICIAN");
        }
    }

    public boolean isDoctor() {
        return this == DOCTOR;
    }

    public boolean isSpecialtyRequired() {
        return this == DOCTOR;
    }
}
