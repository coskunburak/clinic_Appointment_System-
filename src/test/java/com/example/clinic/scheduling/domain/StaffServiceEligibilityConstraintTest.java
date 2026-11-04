package com.example.clinic.scheduling.domain;

import com.example.clinic.common.domain.TimeInterval;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StaffServiceEligibilityConstraintTest {

    private final StaffServiceEligibilityConstraint constraint = new StaffServiceEligibilityConstraint();

    private final UUID staffId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private final UUID serviceId = UUID.randomUUID();
    private final TimeInterval interval = TimeInterval.of(Instant.now(), Instant.now().plus(30, ChronoUnit.MINUTES));

    @Test
    @DisplayName("Positive: Staff eligible for the requested service at the branch succeeds")
    void shouldPassWhenStaffIsEligible() {
        BookingValidationContext context = BookingValidationContext.builder()
                .staffId(staffId)
                .branchId(branchId)
                .serviceId(serviceId)
                .requestedInterval(interval)
                .addEligibility(staffId, branchId, serviceId)
                .build();

        assertThatCode(() -> constraint.validate(context)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Negative: Staff eligible for the service at another branch fails with STAFF_NOT_ELIGIBLE")
    void shouldRejectWhenEligibilityBelongsToAnotherBranch() {
        UUID otherBranchId = UUID.randomUUID();

        BookingValidationContext context = BookingValidationContext.builder()
                .staffId(staffId)
                .branchId(branchId)
                .serviceId(serviceId)
                .requestedInterval(interval)
                .addEligibility(staffId, otherBranchId, serviceId) // eligible at other branch
                .build();

        assertThatThrownBy(() -> constraint.validate(context))
                .isInstanceOf(SchedulingConflictException.class)
                .extracting("errorCode")
                .isEqualTo(SchedulingConflictException.ErrorCode.STAFF_NOT_ELIGIBLE);
    }

    @Test
    @DisplayName("Negative: Staff eligible for a different service at the branch fails with STAFF_NOT_ELIGIBLE")
    void shouldRejectWhenEligibilityBelongsToAnotherService() {
        UUID otherServiceId = UUID.randomUUID();

        BookingValidationContext context = BookingValidationContext.builder()
                .staffId(staffId)
                .branchId(branchId)
                .serviceId(serviceId)
                .requestedInterval(interval)
                .addEligibility(staffId, branchId, otherServiceId) // eligible for other service
                .build();

        assertThatThrownBy(() -> constraint.validate(context))
                .isInstanceOf(SchedulingConflictException.class)
                .extracting("errorCode")
                .isEqualTo(SchedulingConflictException.ErrorCode.STAFF_NOT_ELIGIBLE);
    }

    @Test
    @DisplayName("Negative: Another staff member eligible does not qualify the requested staff member")
    void shouldRejectWhenAnotherStaffIsEligible() {
        UUID otherStaffId = UUID.randomUUID();

        BookingValidationContext context = BookingValidationContext.builder()
                .staffId(staffId)
                .branchId(branchId)
                .serviceId(serviceId)
                .requestedInterval(interval)
                .addEligibility(otherStaffId, branchId, serviceId)
                .build();

        assertThatThrownBy(() -> constraint.validate(context))
                .isInstanceOf(SchedulingConflictException.class)
                .extracting("errorCode")
                .isEqualTo(SchedulingConflictException.ErrorCode.STAFF_NOT_ELIGIBLE);
    }
}
