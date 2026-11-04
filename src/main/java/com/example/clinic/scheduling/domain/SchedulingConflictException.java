package com.example.clinic.scheduling.domain;

/**
 * Domain-specific exception thrown when a candidate appointment violates
 * one or more scheduling business constraints.
 */
public class SchedulingConflictException extends RuntimeException {

    public enum ErrorCode {
        BRANCH_CLOSED,
        STAFF_UNAVAILABLE,
        STAFF_NOT_ELIGIBLE,
        STAFF_OVERLAP
    }

    private final ErrorCode errorCode;

    public SchedulingConflictException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SchedulingConflictException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
