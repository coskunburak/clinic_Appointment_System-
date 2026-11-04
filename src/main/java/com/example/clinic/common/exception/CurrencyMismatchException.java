package com.example.clinic.common.exception;

/**
 * Thrown when an arithmetic or comparison operation is attempted
 * between two Money instances with different currencies.
 */
public class CurrencyMismatchException extends RuntimeException {

    public CurrencyMismatchException(String message) {
        super(message);
    }

    public CurrencyMismatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
