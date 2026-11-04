package com.example.clinic.booking.domain;

/**
 * Domain-specific exception thrown when an illegal appointment state transition,
 * timing invariant violation, or business rule conflict is encountered.
 * <p>
 * Extends {@link IllegalStateException} to preserve standard Java semantic conventions
 * while providing fine-grained domain error codes.
 */
public class AppointmentDomainException extends IllegalStateException {

    private final String errorCode;

    public AppointmentDomainException(String errorCode, String message) {
        super("[" + errorCode + "] " + message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
