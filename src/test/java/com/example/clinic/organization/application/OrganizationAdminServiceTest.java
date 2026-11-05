package com.example.clinic.organization.application;

import com.example.clinic.organization.application.dto.BranchResponse;
import com.example.clinic.organization.application.dto.CreateBranchCommand;
import com.example.clinic.organization.application.dto.CreateDepartmentCommand;
import com.example.clinic.organization.application.dto.CreateOrganizationCommand;
import com.example.clinic.organization.application.dto.DepartmentResponse;
import com.example.clinic.organization.application.dto.OpeningHoursDto;
import com.example.clinic.organization.application.dto.OrganizationResponse;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.Department;
import com.example.clinic.organization.domain.Organization;
import com.example.clinic.organization.infrastructure.BranchOpeningHoursRepository;
import com.example.clinic.organization.infrastructure.BranchRepository;
import com.example.clinic.organization.infrastructure.DepartmentRepository;
import com.example.clinic.organization.infrastructure.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationAdminServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private BranchOpeningHoursRepository branchOpeningHoursRepository;

    @InjectMocks
    private OrganizationAdminService organizationAdminService;

    private Organization activeOrg;
    private Branch activeBranch;

    @BeforeEach
    void setUp() {
        activeOrg = new Organization("Şehir Sağlık Grubu");
        activeBranch = new Branch(activeOrg, "Kadıköy Şubesi", "Europe/Istanbul", "Bağdat Cad.");
    }

    // =========================================================================
    // Organization Tests
    // =========================================================================

    @Test
    @DisplayName("Should create organization with valid command")
    void shouldCreateOrganization() {
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrganizationResponse response = organizationAdminService.createOrganization(
                new CreateOrganizationCommand("Yeni Sağlık Grubu")
        );

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Yeni Sağlık Grubu");
        assertThat(response.active()).isTrue();
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    @DisplayName("Negative: should reject blank organization name in command")
    void shouldRejectBlankOrgName() {
        assertThatThrownBy(() -> new CreateOrganizationCommand("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    @DisplayName("Should update organization name")
    void shouldUpdateOrganization() {
        when(organizationRepository.findById(activeOrg.getId())).thenReturn(Optional.of(activeOrg));

        OrganizationResponse response = organizationAdminService.updateOrganization(activeOrg.getId(), "Güncel Sağlık");

        assertThat(response.name()).isEqualTo("Güncel Sağlık");
        assertThat(activeOrg.getName()).isEqualTo("Güncel Sağlık");
    }

    @Test
    @DisplayName("Negative: should throw when updating nonexistent organization")
    void shouldThrowWhenOrgNotFound() {
        UUID nonExistent = UUID.randomUUID();
        when(organizationRepository.findById(nonExistent)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationAdminService.updateOrganization(nonExistent, "Yeni Ad"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Organization not found");
    }

    // =========================================================================
    // Branch Tests
    // =========================================================================

    @Test
    @DisplayName("Should create branch under active organization")
    void shouldCreateBranch() {
        when(organizationRepository.findById(activeOrg.getId())).thenReturn(Optional.of(activeOrg));
        when(branchRepository.save(any(Branch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BranchResponse response = organizationAdminService.createBranch(
                activeOrg.getId(),
                new CreateBranchCommand("Nişantaşı Şubesi", "Europe/Istanbul", "Valikonağı Cad.")
        );

        assertThat(response.name()).isEqualTo("Nişantaşı Şubesi");
        assertThat(response.timeZone()).isEqualTo("Europe/Istanbul");
        assertThat(response.organizationId()).isEqualTo(activeOrg.getId());
    }

    @Test
    @DisplayName("Negative: should reject branch creation under inactive organization")
    void shouldRejectBranchUnderInactiveOrg() {
        activeOrg.deactivate();
        when(organizationRepository.findById(activeOrg.getId())).thenReturn(Optional.of(activeOrg));

        assertThatThrownBy(() -> organizationAdminService.createBranch(
                activeOrg.getId(),
                new CreateBranchCommand("Yeni Şube", "Europe/Istanbul", "Adres")
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot create branch under inactive organization");

        verify(branchRepository, never()).save(any());
    }

    @Test
    @DisplayName("Negative: should reject invalid IANA time zone identifier")
    void shouldRejectInvalidTimeZone() {
        when(organizationRepository.findById(activeOrg.getId())).thenReturn(Optional.of(activeOrg));

        assertThatThrownBy(() -> organizationAdminService.createBranch(
                activeOrg.getId(),
                new CreateBranchCommand("Yeni Şube", "Invalid/Timezone", "Adres")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid IANA time zone identifier");
    }

    // =========================================================================
    // Department Tests
    // =========================================================================

    @Test
    @DisplayName("Should create department under branch")
    void shouldCreateDepartment() {
        when(branchRepository.findById(activeBranch.getId())).thenReturn(Optional.of(activeBranch));
        when(departmentRepository.findByBranchIdAndName(activeBranch.getId(), "Kardiyoloji")).thenReturn(Optional.empty());
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DepartmentResponse response = organizationAdminService.createDepartment(
                activeBranch.getId(), new CreateDepartmentCommand("Kardiyoloji")
        );

        assertThat(response.name()).isEqualTo("Kardiyoloji");
        assertThat(response.branchId()).isEqualTo(activeBranch.getId());
    }

    @Test
    @DisplayName("Negative: should reject duplicate department name in the same branch")
    void shouldRejectDuplicateDepartmentInSameBranch() {
        when(branchRepository.findById(activeBranch.getId())).thenReturn(Optional.of(activeBranch));
        Department existing = new Department(activeBranch, "Kardiyoloji");
        when(departmentRepository.findByBranchIdAndName(activeBranch.getId(), "Kardiyoloji")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> organizationAdminService.createDepartment(
                activeBranch.getId(), "Kardiyoloji"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists in branch");

        verify(departmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Negative: should reject department creation under inactive branch")
    void shouldRejectDepartmentUnderInactiveBranch() {
        activeBranch.setActive(false);
        when(branchRepository.findById(activeBranch.getId())).thenReturn(Optional.of(activeBranch));

        assertThatThrownBy(() -> organizationAdminService.createDepartment(
                activeBranch.getId(), "Dahiliye"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot create department under inactive branch");
    }

    // =========================================================================
    // Opening Hours Tests
    // =========================================================================

    @Test
    @DisplayName("Should configure valid opening hours with multiple intervals")
    void shouldConfigureValidOpeningHours() {
        when(branchRepository.findById(activeBranch.getId())).thenReturn(Optional.of(activeBranch));

        List<OpeningHoursDto> hours = List.of(
                new OpeningHoursDto(1, LocalTime.of(9, 0), LocalTime.of(12, 0)),
                new OpeningHoursDto(1, LocalTime.of(13, 0), LocalTime.of(17, 0)),
                new OpeningHoursDto(2, LocalTime.of(9, 0), LocalTime.of(17, 0))
        );

        List<OpeningHoursDto> result = organizationAdminService.configureOpeningHours(activeBranch.getId(), hours);

        assertThat(result).hasSize(3);
        verify(branchOpeningHoursRepository).deleteByBranchId(activeBranch.getId());
        verify(branchOpeningHoursRepository).saveAll(any());
    }

    @Test
    @DisplayName("Should accept adjacent non-overlapping intervals on the same weekday")
    void shouldAcceptAdjacentIntervals() {
        when(branchRepository.findById(activeBranch.getId())).thenReturn(Optional.of(activeBranch));

        // 09:00–12:00 and 12:00–17:00 touch at boundary but do NOT overlap
        List<OpeningHoursDto> hours = List.of(
                new OpeningHoursDto(1, LocalTime.of(9, 0), LocalTime.of(12, 0)),
                new OpeningHoursDto(1, LocalTime.of(12, 0), LocalTime.of(17, 0))
        );

        List<OpeningHoursDto> result = organizationAdminService.configureOpeningHours(activeBranch.getId(), hours);
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Negative: should reject overlapping intervals on the same weekday")
    void shouldRejectOverlappingIntervalsOnSameWeekday() {
        when(branchRepository.findById(activeBranch.getId())).thenReturn(Optional.of(activeBranch));

        // 09:00–13:00 and 12:00–17:00 overlap between 12:00 and 13:00
        List<OpeningHoursDto> hours = List.of(
                new OpeningHoursDto(1, LocalTime.of(9, 0), LocalTime.of(13, 0)),
                new OpeningHoursDto(1, LocalTime.of(12, 0), LocalTime.of(17, 0))
        );

        assertThatThrownBy(() -> organizationAdminService.configureOpeningHours(activeBranch.getId(), hours))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Overlapping opening hours on weekday 1");

        // Atomic check: no delete or save should occur
        verify(branchOpeningHoursRepository, never()).deleteByBranchId(any());
        verify(branchOpeningHoursRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Negative: should reject weekday outside 1..7 range in DTO")
    void shouldRejectInvalidWeekday() {
        assertThatThrownBy(() -> new OpeningHoursDto(0, LocalTime.of(9, 0), LocalTime.of(17, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1 (Monday) and 7 (Sunday)");

        assertThatThrownBy(() -> new OpeningHoursDto(8, LocalTime.of(9, 0), LocalTime.of(17, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1 (Monday) and 7 (Sunday)");
    }

    @Test
    @DisplayName("Negative: should reject start_time >= end_time (reversed or zero length) in DTO")
    void shouldRejectInvalidTimeOrder() {
        assertThatThrownBy(() -> new OpeningHoursDto(1, LocalTime.of(17, 0), LocalTime.of(9, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must strictly precede");

        assertThatThrownBy(() -> new OpeningHoursDto(1, LocalTime.of(9, 0), LocalTime.of(9, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must strictly precede");
    }
}
