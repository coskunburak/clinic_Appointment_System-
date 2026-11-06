package com.example.clinic.identity;

import com.example.clinic.AbstractIntegrationTest;
import com.example.clinic.catalog.application.CatalogManagementService;
import com.example.clinic.catalog.application.dto.AssignBranchServiceCommand;
import com.example.clinic.catalog.application.dto.BranchServiceResponse;
import com.example.clinic.catalog.application.dto.CreateServiceCommand;
import com.example.clinic.catalog.application.dto.ServiceDefinitionResponse;
import com.example.clinic.common.security.ClinicPrincipal;
import com.example.clinic.identity.application.AccessPolicy;
import com.example.clinic.identity.application.AccountCredentialService;
import com.example.clinic.identity.domain.Membership;
import com.example.clinic.identity.domain.PatientProfile;
import com.example.clinic.identity.domain.Role;
import com.example.clinic.identity.domain.StaffProfile;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.MembershipRepository;
import com.example.clinic.identity.infrastructure.PatientProfileRepository;
import com.example.clinic.identity.infrastructure.StaffProfileRepository;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import com.example.clinic.organization.application.OrganizationAdminService;
import com.example.clinic.organization.application.dto.BranchResponse;
import com.example.clinic.organization.application.dto.CreateBranchCommand;
import com.example.clinic.organization.application.dto.CreateOrganizationCommand;
import com.example.clinic.organization.application.dto.OpeningHoursDto;
import com.example.clinic.organization.application.dto.OrganizationResponse;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.Organization;
import com.example.clinic.organization.infrastructure.BranchRepository;
import com.example.clinic.organization.infrastructure.OrganizationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end multi-tenant security and authorization integration test suite using real PostgreSQL 17 Testcontainers.
 * Satisfies Phase 9.C, FR-022 through FR-026, NFR-001, NFR-002, and TC-022 through TC-026, TC-037, TC-038.
 */
@Transactional
class SecurityAuthorizationIT extends AbstractIntegrationTest {

    @Autowired
    private OrganizationAdminService organizationAdminService;

    @Autowired
    private CatalogManagementService catalogManagementService;

    @Autowired
    private AccountCredentialService credentialService;

    @Autowired
    private AccessPolicy accessPolicy;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    @Autowired
    private StaffProfileRepository staffProfileRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private BranchRepository branchRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(UserAccount user, Role role) {
        ClinicPrincipal principal = new ClinicPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.isEnabled(),
                List.of(new SimpleGrantedAuthority(role.authority()))
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("TC-025 / TC-037: Passwords persist exclusively as BCrypt hashes in PostgreSQL, never plaintext")
    void passwordsPersistExclusivelyAsHashes() {
        String rawPassword = "ValidPassword123!";
        UserAccount account = credentialService.createAccount("secure_user@clinic.test", rawPassword);

        // Verify direct database persistence
        UserAccount persisted = userAccountRepository.findById(account.getId()).orElseThrow();
        assertThat(persisted.getPasswordHash()).startsWith("$2a$12$");
        assertThat(persisted.getPasswordHash()).doesNotContain(rawPassword);
        assertThat(credentialService.verifyPassword(rawPassword, persisted.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("TC-026 / FR-026 / TC-038: Admin of Organization A cannot modify Organization B (cross-tenant rejection)")
    void adminCannotModifyOtherOrganization() {
        UserAccount adminA = credentialService.createAccount("adminA@clinic.test", "Password123!");
        UserAccount adminB = credentialService.createAccount("adminB@clinic.test", "Password123!");

        // Admin A creates Org A
        authenticateAs(adminA, Role.ROLE_ADMIN);
        OrganizationResponse orgA = organizationAdminService.createOrganization(new CreateOrganizationCommand("Hastane A"));

        // Admin B creates Org B
        authenticateAs(adminB, Role.ROLE_ADMIN);
        OrganizationResponse orgB = organizationAdminService.createOrganization(new CreateOrganizationCommand("Hastane B"));

        // While authenticated as Admin B, attempt to modify Org A
        assertThatThrownBy(() -> organizationAdminService.updateOrganization(orgA.id(), "Sızma Girişimi"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("administrator is not authorized for organization");

        // Org A name must remain unchanged in database
        authenticateAs(adminA, Role.ROLE_ADMIN);
        OrganizationResponse reloaded = organizationAdminService.getOrganization(orgA.id());
        assertThat(reloaded.name()).isEqualTo("Hastane A");
    }

    @Test
    @DisplayName("TC-026 / TC-024: Branch admin cannot configure opening hours or departments of another branch")
    void branchAdminCannotManageOtherBranch() {
        UserAccount mainAdmin = credentialService.createAccount("main_admin@clinic.test", "Password123!");
        UserAccount branchAdmin = credentialService.createAccount("branch1_admin@clinic.test", "Password123!");

        authenticateAs(mainAdmin, Role.ROLE_ADMIN);
        OrganizationResponse org = organizationAdminService.createOrganization(new CreateOrganizationCommand("Kurum Zinciri"));
        BranchResponse branch1 = organizationAdminService.createBranch(org.id(), new CreateBranchCommand("Şube 1", "Europe/Istanbul", null));
        BranchResponse branch2 = organizationAdminService.createBranch(org.id(), new CreateBranchCommand("Şube 2", "Europe/Istanbul", null));

        // Provision branchAdmin strictly for Branch 1
        Branch branch1Entity = branchRepository.findById(branch1.id()).orElseThrow();
        Organization orgEntity = organizationRepository.findById(org.id()).orElseThrow();
        membershipRepository.saveAndFlush(new Membership(branchAdmin, orgEntity, branch1Entity, Role.ROLE_ADMIN));

        authenticateAs(branchAdmin, Role.ROLE_ADMIN);

        // Branch 1 opening hours: allowed
        List<OpeningHoursDto> hours = List.of(new OpeningHoursDto(1, LocalTime.of(9, 0), LocalTime.of(17, 0)));
        assertThatCode(() -> organizationAdminService.configureOpeningHours(branch1.id(), hours))
                .doesNotThrowAnyException();

        // Branch 2 opening hours: rejected at service layer
        assertThatThrownBy(() -> organizationAdminService.configureOpeningHours(branch2.id(), hours))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("administrator is not authorized for branch");
    }

    @Test
    @DisplayName("TC-022 / FR-022: Patient A cannot access Patient B's profile (service IDOR defense)")
    void patientCannotAccessOtherPatientProfile() {
        UserAccount patientA = credentialService.createAccount("patient_a@clinic.test", "Password123!");
        UserAccount patientB = credentialService.createAccount("patient_b@clinic.test", "Password123!");

        PatientProfile profileA = patientProfileRepository.saveAndFlush(new PatientProfile(patientA, "Hasta A", "+905551112233"));
        PatientProfile profileB = patientProfileRepository.saveAndFlush(new PatientProfile(patientB, "Hasta B", "+905554445566"));

        // Patient A accessing own profile
        assertThatCode(() -> accessPolicy.enforcePatientSelfAccess(profileA.getId(), patientA))
                .doesNotThrowAnyException();

        // Patient A accessing Patient B's profile
        assertThatThrownBy(() -> accessPolicy.enforcePatientSelfAccess(profileB.getId(), patientA))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("actor is not the owner of patient profile");
    }

    @Test
    @DisplayName("TC-023 / FR-023: Staff assigned to Branch 1 cannot operate within Branch 2")
    void staffCannotAccessOtherBranch() {
        UserAccount admin = credentialService.createAccount("staff_test_admin@clinic.test", "Password123!");
        UserAccount doctor = credentialService.createAccount("doctor_ayse@clinic.test", "Password123!");

        authenticateAs(admin, Role.ROLE_ADMIN);
        OrganizationResponse org = organizationAdminService.createOrganization(new CreateOrganizationCommand("Hastane"));
        BranchResponse branch1 = organizationAdminService.createBranch(org.id(), new CreateBranchCommand("Kadıköy", "Europe/Istanbul", null));
        BranchResponse branch2 = organizationAdminService.createBranch(org.id(), new CreateBranchCommand("Nişantaşı", "Europe/Istanbul", null));

        Branch branch1Entity = branchRepository.findById(branch1.id()).orElseThrow();
        Organization orgEntity = organizationRepository.findById(org.id()).orElseThrow();

        staffProfileRepository.saveAndFlush(new StaffProfile(doctor, "Dr. Ayşe", null, "DOCTOR", "Kardiyoloji"));
        membershipRepository.saveAndFlush(new Membership(doctor, orgEntity, branch1Entity, Role.ROLE_STAFF));

        // Authorized for Kadıköy
        assertThatCode(() -> accessPolicy.enforceStaffBranchAccess(branch1.id(), doctor))
                .doesNotThrowAnyException();

        // Denied for Nişantaşı
        assertThatThrownBy(() -> accessPolicy.enforceStaffBranchAccess(branch2.id(), doctor))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("staff member is not assigned to branch");
    }

    @Test
    @DisplayName("TC-038 / NFR-002: Direct service invocation without authentication is rejected")
    void directServiceCallWithoutAuthenticationIsRejected() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> organizationAdminService.createOrganization(new CreateOrganizationCommand("İzinsiz Kurum")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Authentication required");
    }

    @Test
    @DisplayName("Revoked membership immediately invalidates authorization on subsequent calls")
    void revokedMembershipImmediatelyAffectsAuthorization() {
        UserAccount admin = credentialService.createAccount("revokable_admin@clinic.test", "Password123!");

        authenticateAs(admin, Role.ROLE_ADMIN);
        OrganizationResponse org = organizationAdminService.createOrganization(new CreateOrganizationCommand("Yetki İptali Test"));

        // Currently authorized
        assertThatCode(() -> organizationAdminService.updateOrganization(org.id(), "Ad 1"))
                .doesNotThrowAnyException();

        // Revoke admin membership directly in DB
        List<Membership> memberships = membershipRepository.findByUserAccountId(admin.getId());
        membershipRepository.deleteAll(memberships);
        membershipRepository.flush();

        // Subsequent call must immediately fail
        assertThatThrownBy(() -> organizationAdminService.updateOrganization(org.id(), "Ad 2"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("administrator is not authorized for organization");
    }

    @Test
    @DisplayName("Catalog service enforces tenant boundary and branch pricing authorization")
    void catalogServiceEnforcesTenantBoundaryAndPricingAuth() {
        UserAccount adminA = credentialService.createAccount("cat_admin_a@clinic.test", "Password123!");
        UserAccount adminB = credentialService.createAccount("cat_admin_b@clinic.test", "Password123!");

        authenticateAs(adminA, Role.ROLE_ADMIN);
        OrganizationResponse orgA = organizationAdminService.createOrganization(new CreateOrganizationCommand("Katalog A"));
        BranchResponse branchA = organizationAdminService.createBranch(orgA.id(), new CreateBranchCommand("Şube A", "Europe/Istanbul", null));
        ServiceDefinitionResponse defA = catalogManagementService.defineService(
                orgA.id(), new CreateServiceCommand("Tahlil A", null)
        );
        BranchServiceResponse bsA = catalogManagementService.assignServiceToBranch(
                branchA.id(), new AssignBranchServiceCommand(defA.id(), 15, new BigDecimal("500.00"), "TRY")
        );

        // Switch to Admin B
        authenticateAs(adminB, Role.ROLE_ADMIN);
        OrganizationResponse orgB = organizationAdminService.createOrganization(new CreateOrganizationCommand("Katalog B"));

        // Admin B cannot update pricing of Branch Service in Org A
        assertThatThrownBy(() -> catalogManagementService.updateServicePricing(bsA.id(), new BigDecimal("999.00")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("administrator is not authorized for branch");
    }
}
