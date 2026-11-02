package com.example.clinic;

import com.example.clinic.identity.domain.Membership;
import com.example.clinic.identity.domain.PatientProfile;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.MembershipRepository;
import com.example.clinic.identity.infrastructure.PatientProfileRepository;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.BranchOpeningHours;
import com.example.clinic.organization.domain.Department;
import com.example.clinic.organization.domain.Organization;
import com.example.clinic.organization.infrastructure.BranchOpeningHoursRepository;
import com.example.clinic.organization.infrastructure.BranchRepository;
import com.example.clinic.organization.infrastructure.DepartmentRepository;
import com.example.clinic.organization.infrastructure.OrganizationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class DatabaseConstraintIT extends AbstractIntegrationTest {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private BranchOpeningHoursRepository branchOpeningHoursRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    @Test
    @DisplayName("Negative: should reject duplicate user email via unique constraint uq_user_account_email")
    void shouldRejectDuplicateEmail() {
        UserAccount first = new UserAccount("duplicate@clinic.test", "hash1");
        userAccountRepository.saveAndFlush(first);

        UserAccount second = new UserAccount("duplicate@clinic.test", "hash2");

        assertThatThrownBy(() -> userAccountRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Negative: should reject duplicate department name in same branch via uq_department_branch_name")
    void shouldRejectDuplicateDepartmentInSameBranch() {
        Organization org = organizationRepository.saveAndFlush(new Organization("Test Org"));
        Branch branch = branchRepository.saveAndFlush(new Branch(org, "Test Branch", "Europe/Istanbul", "Address"));

        Department dept1 = new Department(branch, "Kardiyoloji");
        departmentRepository.saveAndFlush(dept1);

        Department dept2 = new Department(branch, "Kardiyoloji");

        assertThatThrownBy(() -> departmentRepository.saveAndFlush(dept2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Negative: should reject duplicate patient profile for same user via uq_patient_profile_user_id")
    void shouldRejectDuplicatePatientProfileForSameUser() {
        UserAccount user = userAccountRepository.saveAndFlush(new UserAccount("singlepatient@clinic.test", "hash"));

        PatientProfile profile1 = new PatientProfile(user, "Ali Veli", "+905550001122");
        patientProfileRepository.saveAndFlush(profile1);

        PatientProfile profile2 = new PatientProfile(user, "Ali Veli Duplicate", "+905550001133");

        assertThatThrownBy(() -> patientProfileRepository.saveAndFlush(profile2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Negative: should reject cross-tenant membership mismatch via fk_membership_branch_org")
    void shouldRejectCrossTenantBranchMembershipViolation() {
        Organization orgA = organizationRepository.saveAndFlush(new Organization("Org A"));
        Organization orgB = organizationRepository.saveAndFlush(new Organization("Org B"));

        Branch branchOfOrgA = branchRepository.saveAndFlush(new Branch(orgA, "Branch A", "Europe/Istanbul", "Address A"));
        UserAccount user = userAccountRepository.saveAndFlush(new UserAccount("tenantviolator@clinic.test", "hash"));

        // Attempt to create membership referencing Org B but Branch of Org A!
        Membership invalidMembership = new Membership(user, orgB, branchOfOrgA, "ROLE_STAFF");

        assertThatThrownBy(() -> membershipRepository.saveAndFlush(invalidMembership))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Negative: should reject branch opening hours when start_time >= end_time via chk_opening_hours_time_order")
    void shouldRejectInvalidBranchOpeningHoursTimeOrder() {
        Organization org = organizationRepository.saveAndFlush(new Organization("Hours Org"));
        Branch branch = branchRepository.saveAndFlush(new Branch(org, "Hours Branch", "Europe/Istanbul", "Address"));

        // start_time (18:00) is after end_time (09:00), violating CHECK (start_time < end_time)
        BranchOpeningHours invalidHours = new BranchOpeningHours(
                branch,
                (short) 1,
                LocalTime.of(18, 0),
                LocalTime.of(9, 0)
        );

        assertThatThrownBy(() -> branchOpeningHoursRepository.saveAndFlush(invalidHours))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
