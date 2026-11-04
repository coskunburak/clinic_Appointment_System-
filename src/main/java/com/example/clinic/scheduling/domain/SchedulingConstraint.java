package com.example.clinic.scheduling.domain;

/**
 * Common polymorphic strategy interface for scheduling business rules.
 * <p>
 * Demonstrates:
 * <ul>
 *   <li>Interface-based programming & Abstraction</li>
 *   <li>Open/Closed Principle (OCP): New scheduling constraints can be added without modifying the validator.</li>
 *   <li>Single Responsibility Principle (SRP): Each implementing class validates a single domain concern.</li>
 * </ul>
 */
public interface SchedulingConstraint {

    /**
     * Validates the candidate booking against this constraint.
     *
     * @param context the immutable validation context containing relevant verified facts
     * @throws SchedulingConflictException if the booking violates this constraint
     */
    void validate(BookingValidationContext context);
}
