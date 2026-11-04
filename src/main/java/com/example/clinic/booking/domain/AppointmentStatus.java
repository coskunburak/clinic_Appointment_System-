package com.example.clinic.booking.domain;

/**
 * Lifecycle states of an {@link Appointment}.
 * <p>
 * Legal transitions:
 * <ul>
 *   <li>PENDING &rarr; CONFIRMED (approve)</li>
 *   <li>PENDING &rarr; REJECTED (reject)</li>
 *   <li>PENDING &rarr; CANCELLED (cancel)</li>
 *   <li>CONFIRMED &rarr; CANCELLED (cancel)</li>
 *   <li>CONFIRMED &rarr; COMPLETED (complete after endAt)</li>
 * </ul>
 * Terminal states: REJECTED, CANCELLED, COMPLETED.
 */
public enum AppointmentStatus {
    PENDING,
    CONFIRMED,
    REJECTED,
    CANCELLED,
    COMPLETED;

    public boolean isTerminal() {
        return this == REJECTED || this == CANCELLED || this == COMPLETED;
    }

    /**
     * In accordance with ADR-003, active appointments that occupy the staff calendar slot.
     * Only CANCELLED and REJECTED release the scheduling interval.
     */
    public boolean isBlocking() {
        return this == PENDING || this == CONFIRMED || this == COMPLETED;
    }
}
