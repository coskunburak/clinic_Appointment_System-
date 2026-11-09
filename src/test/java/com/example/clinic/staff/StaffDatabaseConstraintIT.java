package com.example.clinic.staff;

import com.example.clinic.AbstractIntegrationTest;
import com.example.clinic.catalog.domain.BranchService;
import com.example.clinic.catalog.domain.ServiceDefinition;
import com.example.clinic.catalog.infrastructure.BranchServiceRepository;
import com.example.clinic.catalog.infrastructure.ServiceDefinitionRepository;
import com.example.clinic.common.domain.Money;
import com.example.clinic.identity.domain.StaffProfile;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.StaffProfileRepository;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.Department;
import com.example.clinic.organization.domain.Organization;
import com.example.clinic.organization.infrastructure.BranchRepository;
import com.example.clinic.organization.infrastructure.DepartmentRepository;
import com.example.clinic.organization.infrastructure.OrganizationRepository;
import com.example.clinic.staff.domain.AvailabilityException;
import com.example.clinic.staff.domain.AvailabilityExceptionKind;
import com.example.clinic.staff.domain.StaffAssignment;
import com.example.clinic.staff.domain.StaffServiceEligibility;
import com.example.clinic.staff.domain.WeeklyAvailability;
import com.example.clinic.staff.infrastructure.AvailabilityExceptionRepository;
import com.example.clinic.staff.infrastructure.StaffAssignmentRepository;
import com.example.clinic.staff.infrastructure.StaffServiceEligibilityRepository;
import com.example.clinic.staff.infrastructure.WeeklyAvailabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class StaffDatabaseConstraintIT extends AbstractIntegrationTest {

    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private BranchRepository branchRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private StaffProfileRepository staffProfileRepository;
    @Autowired
    private ServiceDefinitionRepository serviceDefinitionRepository;
    @Autowired
    private BranchServiceRepository branchServiceRepository;
    @Autowired
    private StaffAssignmentRepository staffAssignmentRepository;
    @Autowired
    private StaffServiceEligibilityRepository staffServiceEligibilityRepository;
    @Autowired
    private WeeklyAvailabilityRepository weeklyAvailabilityRepository;
    @Autowired
    private AvailabilityExceptionRepository availabilityExceptionRepository;

    private Organization org;
    private Branch branch;
    private Department dept;
    private StaffProfile staff;
    private BranchService branchService;
    private StaffAssignment assignment;

    @BeforeEach
    void setUpFixture() {
        org = organizationRepository.saveAndFlush(new Organization("Test Hastanesi"));
        branch = branchRepository.saveAndFlush(new Branch(org, "Merkez Şube", "Europe/Istanbul", "Merkez"));
        dept = departmentRepository.saveAndFlush(new Department(branch, "Kardiyoloji"));

        UserAccount user = userAccountRepository.saveAndFlush(
                new UserAccount("doc_" + UUID.randomUUID() + "@clinic.test", "hash123")
        );
        staff = staffProfileRepository.saveAndFlush(
                new StaffProfile(user, "Dr. Canan", "+905551112233", "DOCTOR", "Kardiyoloji")
        );

        ServiceDefinition def = serviceDefinitionRepository.saveAndFlush(
                new ServiceDefinition(org, "Muayene", "Genel Muayene")
        );
        branchService = branchServiceRepository.saveAndFlush(
                new BranchService(org, branch, def, 30, Money.of(new BigDecimal("500.00"), Currency.getInstance("TRY")))
        );

        assignment = staffAssignmentRepository.saveAndFlush(new StaffAssignment(staff, branch, dept));
    }

    @Test
    @DisplayName("DB Constraint: uq_staff_branch_department prevents duplicate assignment to same department in branch")
    void shouldPreventDuplicateStaffAssignment() {
        assertThatThrownBy(() -> {
            staffAssignmentRepository.saveAndFlush(new StaffAssignment(staff, branch, dept));
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("DB Constraint: uq_assignment_service prevents duplicate service eligibility on assignment")
    void shouldPreventDuplicateServiceEligibility() {
        staffServiceEligibilityRepository.saveAndFlush(new StaffServiceEligibility(assignment, branchService));

        assertThatThrownBy(() -> {
            staffServiceEligibilityRepository.saveAndFlush(new StaffServiceEligibility(assignment, branchService));
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("DB Constraint: weekly_availability persists and cascades")
    void shouldPersistWeeklyAvailability() {
        WeeklyAvailability weekly = weeklyAvailabilityRepository.saveAndFlush(
                new WeeklyAvailability(assignment, (short) 2, LocalTime.of(9, 0), LocalTime.of(17, 0))
        );
        assertThat(weekly.getId()).isNotNull();
        assertThat(weekly.getWeekday()).isEqualTo((short) 2);
    }

    @Test
    @DisplayName("DB Constraint: availability_exception persists full day leave and partial leave")
    void shouldPersistAvailabilityExceptions() {
        AvailabilityException fullDay = availabilityExceptionRepository.saveAndFlush(
                AvailabilityException.createFullDayLeave(assignment, LocalDate.of(2026, 11, 20), "Yıllık İzin")
        );
        AvailabilityException partial = availabilityExceptionRepository.saveAndFlush(
                AvailabilityException.createPartialLeave(
                        assignment, LocalDate.of(2026, 11, 21), LocalTime.of(12, 0), LocalTime.of(14, 0), "Eğitim"
                )
        );

        assertThat(fullDay.getId()).isNotNull();
        assertThat(fullDay.isFullDayLeave()).isTrue();
        assertThat(partial.getId()).isNotNull();
        assertThat(partial.isPartialLeave()).isTrue();
    }
}
