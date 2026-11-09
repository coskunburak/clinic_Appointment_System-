package com.example.clinic.booking.application;

/**
 * Thrown when a booking request conflicts with an existing blocking appointment for the authenticated patient (FR-029).
 * Prevents a patient from double-booking appointments at the same time across different staff members.
 * Corresponds to HTTP 409 Conflict.
 */
public class PatientOverlapException extends RuntimeException {

    public PatientOverlapException(String message) {
        super(message);
    }

    public PatientOverlapException(String message, Throwable cause) {
        super(message, cause);
    }
}
