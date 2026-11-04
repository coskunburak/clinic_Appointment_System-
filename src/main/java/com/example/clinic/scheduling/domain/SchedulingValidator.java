package com.example.clinic.scheduling.domain;

import java.util.List;
import java.util.Objects;

/**
 * Pure Java scheduling policy coordinator that executes a collection of {@link SchedulingConstraint}
 * implementations through their shared polymorphic interface.
 * <p>
 * Key Design Principles:
 * <ul>
 *   <li>Polymorphism: Treats all constraints uniformly via {@code constraint.validate(context)} without instanceof or switches.</li>
 *   <li>Open/Closed Principle (OCP): New constraints participate in validation without modifying this class.</li>
 *   <li>Fail-Fast: Throws immediately upon encountering the first constraint violation.</li>
 *   <li>Defensive Copying: The internal constraint list cannot be mutated after construction.</li>
 * </ul>
 */
public class SchedulingValidator {

    private final List<SchedulingConstraint> constraints;

    /**
     * Constructs a validator with the specified sequence of scheduling constraints.
     * Empty list policy: An empty list of constraints is accepted and indicates that no validation rules apply.
     *
     * @param constraints the list of constraints to execute in deterministic order
     * @throws NullPointerException if the list itself or any individual constraint is null
     */
    public SchedulingValidator(List<SchedulingConstraint> constraints) {
        Objects.requireNonNull(constraints, "Constraints list must not be null");
        for (SchedulingConstraint constraint : constraints) {
            Objects.requireNonNull(constraint, "Individual scheduling constraint must not be null");
        }
        this.constraints = List.copyOf(constraints);
    }

    /**
     * Executes all configured constraints in sequence. Fails immediately on the first conflict.
     *
     * @param context the immutable validation context containing verified scheduling facts
     * @throws SchedulingConflictException if any constraint validation fails
     */
    public void validate(BookingValidationContext context) {
        Objects.requireNonNull(context, "BookingValidationContext must not be null");
        for (SchedulingConstraint constraint : constraints) {
            constraint.validate(context);
        }
    }

    public List<SchedulingConstraint> getConstraints() {
        return constraints;
    }
}
