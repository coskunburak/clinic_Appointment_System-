package com.example.clinic.staff.domain;

import com.example.clinic.catalog.domain.BranchService;
import com.example.clinic.catalog.domain.ServiceDefinition;
import com.example.clinic.common.domain.Money;
import com.example.clinic.identity.domain.StaffProfile;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.Department;
import com.example.clinic.organization.domain.Organization;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StaffEntitiesDomainTest {

    private final Organization org = new Organization(UUID.randomUUID(), "Test Hospital", true, java.time.Instant.now());
    private final Branch branchA = new Branch(UUID.randomUUID(), org, "Kadikoy Branch", "Europe/Istanbul", "Kadikoy", true, java.time.Instant.now());
    private final Branch branchB = new Branch(UUID.randomUUID(), org, "Nisantasi Branch", "Europe/Istanbul", "Nisantasi", true, java.time.Instant.now());
    private final Department deptA = new Department(UUID.randomUUID(), branchA, "Kardiyoloji", true, java.time.Instant.now());
    private final Department deptB = new Department(UUID.randomUUID(), branchB, "Kardiyoloji", true, java.time.Instant.now());
    private final UserAccount user = new UserAccount(UUID.randomUUID(), "dr.ali@clinic.test", "hash123", true, java.time.Instant.now());
    private final StaffProfile staff = new StaffProfile(user, "Dr. Ali Veli", "+905551234567", "DOCTOR", "Kardiyoloji");

    @Test
    @DisplayName("StaffAssignment: Succeeds when department belongs to branch; fails when cross-branch")
    void shouldValidateStaffAssignmentBranchDepartmentMatch() {
        StaffAssignment valid = new StaffAssignment(staff, branchA, deptA);
        assertThat(valid.getStaff()).isEqualTo(staff);
        assertThat(valid.getBranch()).isEqualTo(branchA);
        assertThat(valid.getDepartment()).isEqualTo(deptA);
        assertThat(valid.isActive()).isTrue();

        valid.deactivate();
        assertThat(valid.isActive()).isFalse();

        assertThatThrownBy(() -> new StaffAssignment(staff, branchA, deptB))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to branch");
    }

    @Test
    @DisplayName("StaffServiceEligibility: Succeeds when branch service belongs to assignment branch; fails otherwise")
    void shouldValidateStaffServiceEligibilityBranchMatch() {
        StaffAssignment assignmentA = new StaffAssignment(staff, branchA, deptA);

        ServiceDefinition serviceDef = new ServiceDefinition(UUID.randomUUID(), org, "Eko Cekimi", "Ekokardiyografi", true, java.time.Instant.now());
        BranchService serviceInBranchA = new BranchService(
                UUID.randomUUID(), org, branchA, serviceDef, 30, Money.of(new BigDecimal("1000.00"), Currency.getInstance("TRY")), true, java.time.Instant.now()
        );
        BranchService serviceInBranchB = new BranchService(
                UUID.randomUUID(), org, branchB, serviceDef, 30, Money.of(new BigDecimal("1200.00"), Currency.getInstance("TRY")), true, java.time.Instant.now()
        );

        StaffServiceEligibility validEligibility = new StaffServiceEligibility(assignmentA, serviceInBranchA);
        assertThat(validEligibility.getAssignment()).isEqualTo(assignmentA);
        assertThat(validEligibility.getBranchService()).isEqualTo(serviceInBranchA);

        assertThatThrownBy(() -> new StaffServiceEligibility(assignmentA, serviceInBranchB))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match assignment branch");
    }

    @Test
    @DisplayName("WeeklyAvailability: Validates weekday 1..7 and startTime < endTime")
    void shouldValidateWeeklyAvailabilityInvariants() {
        StaffAssignment assignment = new StaffAssignment(staff, branchA, deptA);

        WeeklyAvailability valid = new WeeklyAvailability(assignment, (short) 1, LocalTime.of(9, 0), LocalTime.of(17, 0));
        assertThat(valid.getWeekday()).isEqualTo((short) 1);
        assertThat(valid.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);

        // Invalid weekday
        assertThatThrownBy(() -> new WeeklyAvailability(assignment, (short) 0, LocalTime.of(9, 0), LocalTime.of(17, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weekday must be between 1 and 7");

        assertThatThrownBy(() -> new WeeklyAvailability(assignment, (short) 8, LocalTime.of(9, 0), LocalTime.of(17, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weekday must be between 1 and 7");

        // startTime not before endTime
        assertThatThrownBy(() -> new WeeklyAvailability(assignment, (short) 1, LocalTime.of(17, 0), LocalTime.of(9, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be before endTime");

        assertThatThrownBy(() -> new WeeklyAvailability(assignment, (short) 1, LocalTime.of(9, 0), LocalTime.of(9, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be before endTime");
    }

    @Test
    @DisplayName("AvailabilityException: Validates full day leave vs partial leave / overtime")
    void shouldValidateAvailabilityExceptionInvariants() {
        StaffAssignment assignment = new StaffAssignment(staff, branchA, deptA);
        LocalDate date = LocalDate.of(2026, 11, 10);

        // Full day leave allows null times
        AvailabilityException fullDay = AvailabilityException.createFullDayLeave(assignment, date, "Yillik izin");
        assertThat(fullDay.isFullDayLeave()).isTrue();
        assertThat(fullDay.getStartTime()).isNull();
        assertThat(fullDay.getEndTime()).isNull();

        // Full day leave with times is rejected
        assertThatThrownBy(() -> new AvailabilityException(assignment, date, AvailabilityExceptionKind.FULL_DAY_LEAVE,
                LocalTime.of(9, 0), LocalTime.of(17, 0), "Hatali"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Full-day leave must not specify start or end times");

        // Partial leave requires valid times
        AvailabilityException partial = AvailabilityException.createPartialLeave(
                assignment, date, LocalTime.of(12, 0), LocalTime.of(13, 0), "Ogle izni"
        );
        assertThat(partial.isPartialLeave()).isTrue();
        assertThat(partial.getStartTime()).isEqualTo(LocalTime.of(12, 0));

        assertThatThrownBy(() -> AvailabilityException.createPartialLeave(assignment, date, null, LocalTime.of(13, 0), "Eksik"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> AvailabilityException.createPartialLeave(assignment, date, LocalTime.of(14, 0), LocalTime.of(13, 0), "Ters"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be before endTime");
    }
}
