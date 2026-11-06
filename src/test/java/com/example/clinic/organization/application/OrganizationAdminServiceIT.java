package com.example.clinic.organization.application;

import com.example.clinic.AbstractIntegrationTest;
import com.example.clinic.common.security.ClinicPrincipal;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import com.example.clinic.organization.application.dto.BranchResponse;
import com.example.clinic.organization.application.dto.CreateBranchCommand;
import com.example.clinic.organization.application.dto.CreateDepartmentCommand;
import com.example.clinic.organization.application.dto.CreateOrganizationCommand;
import com.example.clinic.organization.application.dto.DepartmentResponse;
import com.example.clinic.organization.application.dto.OpeningHoursDto;
import com.example.clinic.organization.application.dto.OrganizationResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class OrganizationAdminServiceIT extends AbstractIntegrationTest {

    @Autowired
    private OrganizationAdminService organizationAdminService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private UserAccount testAdmin;

    @BeforeEach
    void setUpSecurityActor() {
        testAdmin = userAccountRepository.saveAndFlush(
                new UserAccount("admin_it_" + java.util.UUID.randomUUID() + "@clinic.test", "hash123")
        );
        ClinicPrincipal principal = new ClinicPrincipal(
                testAdmin.getId(),
                testAdmin.getEmail(),
                testAdmin.getPasswordHash(),
                testAdmin.isEnabled(),
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("TC-001 / FR-001: Admin creates and updates organization, persists and reads back correctly")
    void shouldCreateAndUpdateOrganization() {
        OrganizationResponse created = organizationAdminService.createOrganization(
                new CreateOrganizationCommand("İstanbul Sağlık A.Ş.")
        );

        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo("İstanbul Sağlık A.Ş.");
        assertThat(created.active()).isTrue();

        OrganizationResponse updated = organizationAdminService.updateOrganization(
                created.id(), "İstanbul Sağlık Grubu"
        );
        assertThat(updated.name()).isEqualTo("İstanbul Sağlık Grubu");

        OrganizationResponse retrieved = organizationAdminService.getOrganization(created.id());
        assertThat(retrieved.name()).isEqualTo("İstanbul Sağlık Grubu");
    }

    @Test
    @DisplayName("TC-002 / FR-002: Single-branch and multi-branch support under the same organization")
    void shouldSupportSingleAndMultiBranchOrganizations() {
        OrganizationResponse org = organizationAdminService.createOrganization(
                new CreateOrganizationCommand("Anadolu Tıp Grubu")
        );

        // 1. Single-branch scenario
        BranchResponse branch1 = organizationAdminService.createBranch(
                org.id(),
                new CreateBranchCommand("Kadıköy Şubesi", "Europe/Istanbul", "Bağdat Cad. No: 100")
        );

        List<BranchResponse> singleBranchList = organizationAdminService.getBranchesByOrganization(org.id());
        assertThat(singleBranchList).hasSize(1);
        assertThat(singleBranchList.getFirst().name()).isEqualTo("Kadıköy Şubesi");

        // 2. Multi-branch addition without code modification
        BranchResponse branch2 = organizationAdminService.createBranch(
                org.id(),
                new CreateBranchCommand("Nişantaşı Şubesi", "Europe/Istanbul", "Valikonağı Cad. No: 50")
        );

        List<BranchResponse> multiBranchList = organizationAdminService.getBranchesByOrganization(org.id());
        assertThat(multiBranchList).hasSize(2);
        assertThat(multiBranchList).extracting(BranchResponse::name)
                .containsExactlyInAnyOrder("Kadıköy Şubesi", "Nişantaşı Şubesi");
    }

    @Test
    @DisplayName("TC-003 / FR-003: Configure weekly branch opening hours with multiple intervals and atomic replacement")
    void shouldConfigureWeeklyOpeningHoursAtomically() {
        OrganizationResponse org = organizationAdminService.createOrganization(
                new CreateOrganizationCommand("Mesai Test Kurumu")
        );
        BranchResponse branch = organizationAdminService.createBranch(
                org.id(),
                new CreateBranchCommand("Beşiktaş Şubesi", "Europe/Istanbul", "Barbaros Bulvarı")
        );

        // Monday split schedule (09:00–12:00, 13:00–17:00) and Tuesday full schedule (09:00–17:00)
        List<OpeningHoursDto> initialSchedule = List.of(
                new OpeningHoursDto(1, LocalTime.of(9, 0), LocalTime.of(12, 0)),
                new OpeningHoursDto(1, LocalTime.of(13, 0), LocalTime.of(17, 0)),
                new OpeningHoursDto(2, LocalTime.of(9, 0), LocalTime.of(17, 0))
        );

        organizationAdminService.configureOpeningHours(branch.id(), initialSchedule);

        List<OpeningHoursDto> retrieved = organizationAdminService.getOpeningHoursByBranch(branch.id());
        assertThat(retrieved).hasSize(3);

        // Atomic replacement with updated schedule
        List<OpeningHoursDto> updatedSchedule = List.of(
                new OpeningHoursDto(1, LocalTime.of(8, 30), LocalTime.of(18, 0)),
                new OpeningHoursDto(3, LocalTime.of(9, 0), LocalTime.of(13, 0))
        );

        organizationAdminService.configureOpeningHours(branch.id(), updatedSchedule);

        List<OpeningHoursDto> updatedRetrieved = organizationAdminService.getOpeningHoursByBranch(branch.id());
        assertThat(updatedRetrieved).hasSize(2);
        assertThat(updatedRetrieved.getFirst().weekday()).isEqualTo((short) 1);
        assertThat(updatedRetrieved.getFirst().startTime()).isEqualTo(LocalTime.of(8, 30));
    }

    @Test
    @DisplayName("TC-004 / FR-004: Define departments under branch, enforce per-branch uniqueness, allow same name across branches")
    void shouldManageDepartmentsAcrossBranches() {
        OrganizationResponse org = organizationAdminService.createOrganization(
                new CreateOrganizationCommand("Bölüm Test Kurumu")
        );
        BranchResponse branchA = organizationAdminService.createBranch(
                org.id(), new CreateBranchCommand("Kadıköy", "Europe/Istanbul", null)
        );
        BranchResponse branchB = organizationAdminService.createBranch(
                org.id(), new CreateBranchCommand("Nişantaşı", "Europe/Istanbul", null)
        );

        DepartmentResponse deptA1 = organizationAdminService.createDepartment(branchA.id(), "Kardiyoloji");
        DepartmentResponse deptA2 = organizationAdminService.createDepartment(branchA.id(), "Radyoloji");

        assertThat(deptA1.name()).isEqualTo("Kardiyoloji");
        assertThat(deptA2.name()).isEqualTo("Radyoloji");

        // Duplicate department name in the same branch should be rejected
        assertThatThrownBy(() -> organizationAdminService.createDepartment(branchA.id(), "Kardiyoloji"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists in branch");

        // Same department name in different branch should be allowed
        DepartmentResponse deptB1 = organizationAdminService.createDepartment(branchB.id(), "Kardiyoloji");
        assertThat(deptB1.id()).isNotNull();
        assertThat(deptB1.branchId()).isEqualTo(branchB.id());

        List<DepartmentResponse> branchADepts = organizationAdminService.getDepartmentsByBranch(branchA.id());
        assertThat(branchADepts).hasSize(2);

        List<DepartmentResponse> branchBDepts = organizationAdminService.getDepartmentsByBranch(branchB.id());
        assertThat(branchBDepts).hasSize(1);
    }
}
