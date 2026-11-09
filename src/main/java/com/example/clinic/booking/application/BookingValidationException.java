package com.example.clinic.booking.application;

/**
 * Thrown when a booking request violates business preconditions or schedule constraints
 * (e.g. branch closed, staff on leave, inactive entity, cross-tenant mismatch, DST gap).
 * Corresponds to HTTP 400 Bad Request or HTTP 422 Unprocessable Entity.
 */
public class BookingValidationException extends RuntimeException {

    public BookingValidationException(String message) {
        super(message);
    }

    public BookingValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
