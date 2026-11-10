package com.example.clinic.identity.application;

import com.example.clinic.identity.domain.Membership;
import com.example.clinic.identity.domain.PatientProfile;
import com.example.clinic.identity.domain.Role;
import com.example.clinic.identity.domain.StaffProfile;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.MembershipRepository;
import com.example.clinic.identity.infrastructure.PatientProfileRepository;
import com.example.clinic.identity.infrastructure.StaffProfileRepository;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.Organization;
import com.example.clinic.organization.infrastructure.BranchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessPolicyTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @Mock
    private StaffProfileRepository staffProfileRepository;

    @Mock
    private BranchRepository branchRepository;

    @InjectMocks
    private AccessPolicy accessPolicy;

    private Organization org1;
    private Organization org2;
    private Branch branch1;
    private Branch branch2;
    private Branch branchOtherOrg;

    @BeforeEach
    void setUp() {
        org1 = new Organization("Klinik 1");
        org2 = new Organization("Klinik 2");
        branch1 = new Branch(org1, "Şube 1", "Europe/Istanbul", null);
        branch2 = new Branch(org1, "Şube 2", "Europe/Istanbul", null);
        branchOtherOrg = new Branch(org2, "Şube Farklı Kurum", "Europe/Istanbul", null);
    }

    // =========================================================================
    // 1. Patient Self-Access (TC-022 / FR-022)
    // =========================================================================

    @Test
    @DisplayName("TC-022 / FR-022: Patient accessing own profile must be authorized")
    void shouldAllowPatientSelfAccess() {
        UserAccount patientUser = new UserAccount("patient@clinic.test", "hash");
        PatientProfile profile = new PatientProfile(patientUser, "Ali Veli", "+905551112233");

        when(patientProfileRepository.findById(profile.getId())).thenReturn(Optional.of(profile));

        assertThatCode(() -> accessPolicy.enforcePatientSelfAccess(profile.getId(), patientUser))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("TC-022 / FR-022: Patient A attempting to access Patient B's profile must be rejected (IDOR defense)")
    void shouldRejectCrossPatientAccess() {
        UserAccount patientA = new UserAccount("patientA@clinic.test", "hash");
        UserAccount patientB = new UserAccount("patientB@clinic.test", "hash");
        PatientProfile profileB = new PatientProfile(patientB, "Ayşe Fatma", "+905552223344");

        when(patientProfileRepository.findById(profileB.getId())).thenReturn(Optional.of(profileB));

        assertThatThrownBy(() -> accessPolicy.enforcePatientSelfAccess(profileB.getId(), patientA))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("actor is not the owner of patient profile");
    }

    @Test
    @DisplayName("Negative: disabled patient account must be rejected")
    void shouldRejectDisabledPatientAccess() {
        UserAccount patientUser = new UserAccount("patient@clinic.test", "hash");
        patientUser.setEnabled(false);
        PatientProfile profile = new PatientProfile(patientUser, "Ali Veli", "+905551112233");

        assertThatThrownBy(() -> accessPolicy.enforcePatientSelfAccess(profile.getId(), patientUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("user account is disabled");
    }

    // =========================================================================
    // 2. Staff Branch Scoping (TC-023 / FR-023)
    // =========================================================================

    @Test
    @DisplayName("TC-023 / FR-023: Staff assigned to branch must be authorized for that branch")
    void shouldAllowStaffAssignedBranchAccess() {
        UserAccount staffUser = new UserAccount("doctor@clinic.test", "hash");
        StaffProfile staffProfile = new StaffProfile(staffUser, "Dr. Mehmet", null, "DOCTOR", "Kardiyoloji");
        Membership membership = new Membership(staffUser, org1, branch1, Role.ROLE_STAFF);

        when(staffProfileRepository.findByUserAccountId(staffUser.getId())).thenReturn(Optional.of(staffProfile));
        when(membershipRepository.findByUserAccountIdAndRole(staffUser.getId(), Role.ROLE_STAFF))
                .thenReturn(List.of(membership));

        assertThatCode(() -> accessPolicy.enforceStaffBranchAccess(branch1.getId(), staffUser))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("TC-023 / FR-023: Staff assigned to Branch 1 attempting to access Branch 2 must be rejected")
    void shouldRejectCrossBranchStaffAccess() {
        UserAccount staffUser = new UserAccount("doctor@clinic.test", "hash");
        StaffProfile staffProfile = new StaffProfile(staffUser, "Dr. Mehmet", null, "DOCTOR", "Kardiyoloji");
        Membership membership = new Membership(staffUser, org1, branch1, Role.ROLE_STAFF);

        when(staffProfileRepository.findByUserAccountId(staffUser.getId())).thenReturn(Optional.of(staffProfile));
        when(membershipRepository.findByUserAccountIdAndRole(staffUser.getId(), Role.ROLE_STAFF))
                .thenReturn(List.of(membership));

        assertThatThrownBy(() -> accessPolicy.enforceStaffBranchAccess(branch2.getId(), staffUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("staff member is not assigned to branch");
    }

    @Test
    @DisplayName("Negative: staff member with inactive profile must be rejected")
    void shouldRejectInactiveStaffAccess() {
        UserAccount staffUser = new UserAccount("doctor@clinic.test", "hash");
        StaffProfile staffProfile = new StaffProfile(staffUser, "Dr. Mehmet", null, "DOCTOR", "Kardiyoloji");
        staffProfile.setActive(false);

        when(staffProfileRepository.findByUserAccountId(staffUser.getId())).thenReturn(Optional.of(staffProfile));

        assertThatThrownBy(() -> accessPolicy.enforceStaffBranchAccess(branch1.getId(), staffUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("staff profile is inactive");
    }

    // =========================================================================
    // 3. Admin Organization and Branch Scoping (TC-024 / TC-026 / FR-024 / FR-026)
    // =========================================================================

    @Test
    @DisplayName("TC-024 / FR-024: Organization-wide admin authorized for their organization")
    void shouldAllowOrgWideAdminAccess() {
        UserAccount adminUser = new UserAccount("admin@clinic.test", "hash");
        Membership membership = new Membership(adminUser, org1, null, Role.ROLE_ADMIN);

        when(membershipRepository.findByUserAccountIdAndRole(adminUser.getId(), Role.ROLE_ADMIN))
                .thenReturn(List.of(membership));

        assertThatCode(() -> accessPolicy.enforceAdminOrganizationAccess(org1.getId(), adminUser))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("TC-026 / FR-026: Admin of Org 1 attempting to manage Org 2 must be rejected")
    void shouldRejectCrossOrgAdminAccess() {
        UserAccount adminUser = new UserAccount("admin@clinic.test", "hash");
        Membership membership = new Membership(adminUser, org1, null, Role.ROLE_ADMIN);

        when(membershipRepository.findByUserAccountIdAndRole(adminUser.getId(), Role.ROLE_ADMIN))
                .thenReturn(List.of(membership));

        assertThatThrownBy(() -> accessPolicy.enforceAdminOrganizationAccess(org2.getId(), adminUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("administrator is not authorized for organization");
    }

    @Test
    @DisplayName("Negative: branch-scoped admin membership cannot perform organization-level operation")
    void shouldRejectBranchAdminForOrgLevelOperation() {
        UserAccount branchAdmin = new UserAccount("branch_admin@clinic.test", "hash");
        Membership membership = new Membership(branchAdmin, org1, branch1, Role.ROLE_ADMIN);

        when(membershipRepository.findByUserAccountIdAndRole(branchAdmin.getId(), Role.ROLE_ADMIN))
                .thenReturn(List.of(membership));

        assertThatThrownBy(() -> accessPolicy.enforceAdminOrganizationAccess(org1.getId(), branchAdmin))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("administrator membership is restricted to specific branch");
    }

    @Test
    @DisplayName("TC-024: Branch-scoped admin authorized for assigned branch, rejected for sibling branch")
    void shouldAllowBranchAdminForOwnBranchAndRejectSiblingBranch() {
        UserAccount branchAdmin = new UserAccount("branch_admin@clinic.test", "hash");
        Membership membership = new Membership(branchAdmin, org1, branch1, Role.ROLE_ADMIN);

        when(branchRepository.findById(branch1.getId())).thenReturn(Optional.of(branch1));
        when(branchRepository.findById(branch2.getId())).thenReturn(Optional.of(branch2));
        when(membershipRepository.findByUserAccountIdAndRole(branchAdmin.getId(), Role.ROLE_ADMIN))
                .thenReturn(List.of(membership));

        // Allowed for Branch 1
        assertThatCode(() -> accessPolicy.enforceAdminBranchAccess(branch1.getId(), branchAdmin))
                .doesNotThrowAnyException();

        // Rejected for sibling Branch 2 in the same organization
        assertThatThrownBy(() -> accessPolicy.enforceAdminBranchAccess(branch2.getId(), branchAdmin))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("administrator is not authorized for branch");
    }

    @Test
    @DisplayName("TC-026: Admin of Org 1 attempting to access branch of Org 2 must be rejected")
    void shouldRejectAdminAccessToBranchOfOtherOrg() {
        UserAccount admin1 = new UserAccount("admin1@clinic.test", "hash");
        Membership membership = new Membership(admin1, org1, null, Role.ROLE_ADMIN);

        when(branchRepository.findById(branchOtherOrg.getId())).thenReturn(Optional.of(branchOtherOrg));
        when(membershipRepository.findByUserAccountIdAndRole(admin1.getId(), Role.ROLE_ADMIN))
                .thenReturn(List.of(membership));

        assertThatThrownBy(() -> accessPolicy.enforceAdminBranchAccess(branchOtherOrg.getId(), admin1))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("administrator is not authorized for branch");
    }

    @Test
    @DisplayName("S07 / FR-018: hasAdminBranchAccess returns true for authorized admin and false for unauthorized/disabled/missing branch")
    void testHasAdminBranchAccessScenarios() {
        UserAccount branchAdmin = new UserAccount("branch_admin@clinic.test", "hash");
        Membership membership = new Membership(branchAdmin, org1, branch1, Role.ROLE_ADMIN);

        when(branchRepository.findById(branch1.getId())).thenReturn(Optional.of(branch1));
        when(branchRepository.findById(branch2.getId())).thenReturn(Optional.of(branch2));
        when(membershipRepository.findByUserAccountIdAndRole(branchAdmin.getId(), Role.ROLE_ADMIN))
                .thenReturn(List.of(membership));
        when(userAccountRepository.findById(branchAdmin.getId())).thenReturn(Optional.of(branchAdmin));

        // Authorized branch
        assertThat(accessPolicy.hasAdminBranchAccess(branch1.getId(), branchAdmin)).isTrue();
        assertThat(accessPolicy.hasAdminBranchAccess(branch1.getId(), branchAdmin.getId())).isTrue();

        // Unauthorized sibling branch
        assertThat(accessPolicy.hasAdminBranchAccess(branch2.getId(), branchAdmin)).isFalse();
        assertThat(accessPolicy.hasAdminBranchAccess(branch2.getId(), branchAdmin.getId())).isFalse();

        // Unknown branch
        UUID unknownBranchId = UUID.randomUUID();
        when(branchRepository.findById(unknownBranchId)).thenReturn(Optional.empty());
        assertThat(accessPolicy.hasAdminBranchAccess(unknownBranchId, branchAdmin)).isFalse();

        // Null / disabled checks
        assertThat(accessPolicy.hasAdminBranchAccess(null, branchAdmin)).isFalse();
        assertThat(accessPolicy.hasAdminBranchAccess(branch1.getId(), (UserAccount) null)).isFalse();
        assertThat(accessPolicy.hasAdminBranchAccess(branch1.getId(), (UUID) null)).isFalse();
    }
}
