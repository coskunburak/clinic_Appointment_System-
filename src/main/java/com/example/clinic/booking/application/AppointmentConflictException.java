package com.example.clinic.booking.application;

/**
 * Thrown when a booking request conflicts with an existing blocking appointment for the chosen staff member.
 * Corresponds to HTTP 409 Conflict.
 */
public class AppointmentConflictException extends RuntimeException {

    public AppointmentConflictException(String message) {
        super(message);
    }

    public AppointmentConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
