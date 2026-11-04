package com.example.clinic.scheduling.domain;

import com.example.clinic.common.domain.TimeInterval;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SchedulingValidatorTest {

    private BookingValidationContext dummyContext() {
        return BookingValidationContext.builder()
                .staffId(UUID.randomUUID())
                .branchId(UUID.randomUUID())
                .serviceId(UUID.randomUUID())
                .requestedInterval(TimeInterval.of(Instant.now(), Instant.now().plus(30, ChronoUnit.MINUTES)))
                .build();
    }

    @Test
    @DisplayName("Positive: Empty constraints list performs no validation and succeeds")
    void shouldAcceptEmptyConstraintsList() {
        SchedulingValidator validator = new SchedulingValidator(List.of());
        assertThat(validator.getConstraints()).isEmpty();
        assertThatCode(() -> validator.validate(dummyContext())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Negative: Null constraints list is rejected with NullPointerException")
    void shouldRejectNullConstraintsList() {
        assertThatThrownBy(() -> new SchedulingValidator(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Constraints list must not be null");
    }

    @Test
    @DisplayName("Negative: List containing a null constraint is rejected with NullPointerException")
    void shouldRejectListWithNullConstraint() {
        List<SchedulingConstraint> list = new ArrayList<>();
        list.add(new OpeningHoursConstraint());
        list.add(null);

        assertThatThrownBy(() -> new SchedulingValidator(list))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Individual scheduling constraint must not be null");
    }

    @Test
    @DisplayName("Positive: Defensive copying protects internal constraints list from external mutation")
    void shouldDefensivelyCopyConstraintsList() {
        List<SchedulingConstraint> mutableList = new ArrayList<>();
        mutableList.add(new OpeningHoursConstraint());

        SchedulingValidator validator = new SchedulingValidator(mutableList);
        mutableList.clear(); // mutate original list

        assertThat(validator.getConstraints()).hasSize(1);
    }

    @Test
    @DisplayName("Positive: Fail-fast halts execution on first failing constraint")
    void shouldFailFastOnFirstViolatedConstraint() {
        AtomicBoolean secondExecuted = new AtomicBoolean(false);

        SchedulingConstraint failing = ctx -> {
            throw new SchedulingConflictException(
                    SchedulingConflictException.ErrorCode.BRANCH_CLOSED,
                    "Branch closed"
            );
        };

        SchedulingConstraint second = ctx -> secondExecuted.set(true);

        SchedulingValidator validator = new SchedulingValidator(List.of(failing, second));

        assertThatThrownBy(() -> validator.validate(dummyContext()))
                .isInstanceOf(SchedulingConflictException.class)
                .extracting("errorCode")
                .isEqualTo(SchedulingConflictException.ErrorCode.BRANCH_CLOSED);

        assertThat(secondExecuted.get()).isFalse(); // Second constraint was not invoked
    }

    @Test
    @DisplayName("Positive: Sequential execution of multiple passing constraints")
    void shouldExecuteAllPassingConstraintsDeterministically() {
        List<String> executionLog = new ArrayList<>();

        SchedulingConstraint c1 = ctx -> executionLog.add("C1");
        SchedulingConstraint c2 = ctx -> executionLog.add("C2");
        SchedulingConstraint c3 = ctx -> executionLog.add("C3");

        SchedulingValidator validator = new SchedulingValidator(List.of(c1, c2, c3));
        validator.validate(dummyContext());

        assertThat(executionLog).containsExactly("C1", "C2", "C3");
    }
}
