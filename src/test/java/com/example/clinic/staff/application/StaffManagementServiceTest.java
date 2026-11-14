package com.example.clinic.staff.application;

import com.example.clinic.catalog.domain.BranchService;
import com.example.clinic.catalog.domain.ServiceDefinition;
import com.example.clinic.catalog.infrastructure.BranchServiceRepository;
import com.example.clinic.common.domain.Money;
import com.example.clinic.common.security.SecurityContextFacade;
import com.example.clinic.identity.application.AccessPolicy;
import com.example.clinic.identity.application.AccountCredentialService;
import com.example.clinic.identity.domain.Membership;
import com.example.clinic.identity.domain.Role;
import com.example.clinic.identity.domain.StaffProfile;
import com.example.clinic.identity.domain.StaffType;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.MembershipRepository;
import com.example.clinic.identity.infrastructure.StaffProfileRepository;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.Department;
import com.example.clinic.organization.domain.Organization;
import com.example.clinic.organization.infrastructure.BranchRepository;
import com.example.clinic.organization.infrastructure.DepartmentRepository;
import com.example.clinic.staff.application.dto.AssignStaffCommand;
import com.example.clinic.staff.application.dto.AvailabilityExceptionResponse;
import com.example.clinic.staff.application.dto.ConfigureEligibilityCommand;
import com.example.clinic.staff.application.dto.ConfigureWeeklyAvailabilityCommand;
import com.example.clinic.staff.application.dto.CreateAvailabilityExceptionCommand;
import com.example.clinic.staff.application.dto.CreateStaffCommand;
import com.example.clinic.staff.application.dto.StaffAssignmentResponse;
import com.example.clinic.staff.application.dto.StaffProfileResponse;
import com.example.clinic.staff.application.dto.StaffServiceEligibilityResponse;
import com.example.clinic.staff.application.dto.WeeklyAvailabilityItem;
import com.example.clinic.staff.application.dto.WeeklyAvailabilityResponse;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffManagementServiceTest {

    @Mock
    private StaffProfileRepository staffProfileRepository;
    @Mock
    private StaffAssignmentRepository staffAssignmentRepository;
    @Mock
    private StaffServiceEligibilityRepository staffServiceEligibilityRepository;
    @Mock
    private WeeklyAvailabilityRepository weeklyAvailabilityRepository;
    @Mock
    private AvailabilityExceptionRepository availabilityExceptionRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private BranchServiceRepository branchServiceRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private AccountCredentialService accountCredentialService;
    @Mock
    private AccessPolicy accessPolicy;
    @Mock
    private SecurityContextFacade securityContextFacade;

    private StaffManagementService service;

    private UserAccount adminActor;
    private Organization organization;
    private Branch branch;
    private Department department;
    private UserAccount staffUser;
    private StaffProfile staffProfile;
    private StaffAssignment assignment;

    @BeforeEach
    void setUp() {
        service = new StaffManagementService(
                staffProfileRepository,
                staffAssignmentRepository,
                staffServiceEligibilityRepository,
                weeklyAvailabilityRepository,
                availabilityExceptionRepository,
                branchRepository,
                departmentRepository,
                branchServiceRepository,
                userAccountRepository,
                membershipRepository,
                accountCredentialService,
                accessPolicy,
                securityContextFacade
        );

        adminActor = new UserAccount(UUID.randomUUID(), "admin@clinic.test", "hash_admin", true, Instant.now());
        organization = new Organization(UUID.randomUUID(), "Acibadem", true, Instant.now());
        branch = new Branch(UUID.randomUUID(), organization, "Kadikoy", "Europe/Istanbul", "Moda Cad.", true, Instant.now());
        department = new Department(UUID.randomUUID(), branch, "Kardiyoloji", true, Instant.now());

        staffUser = new UserAccount(UUID.randomUUID(), "doc@clinic.test", "hash_staff", true, Instant.now());
        staffProfile = new StaffProfile(staffUser, "Dr. Mehmet Kaya", "+905551112233", "DOCTOR", "Kardiyoloji");
        assignment = new StaffAssignment(staffProfile, branch, department);
    }

    @Test
    @DisplayName("registerStaff: Successfully registers a doctor with specialty and provisions staff membership")
    void shouldRegisterDoctorWithSpecialty() {
        CreateStaffCommand cmd = new CreateStaffCommand(
                organization.getId(), branch.getId(), "doc@clinic.test", "StrongPass123",
                "Dr. Mehmet Kaya", "+905551112233", "DOCTOR", "Kardiyoloji"
        );

        when(branchRepository.findById(branch.getId())).thenReturn(Optional.of(branch));
        when(userAccountRepository.findByEmail("doc@clinic.test")).thenReturn(Optional.empty());
        when(accountCredentialService.createAccount("doc@clinic.test", "StrongPass123")).thenReturn(staffUser);
        when(staffProfileRepository.save(any(StaffProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(membershipRepository.findByUserAccountIdAndRole(staffUser.getId(), Role.ROLE_STAFF))
                .thenReturn(Collections.emptyList());

        StaffProfileResponse response = service.registerStaff(cmd, adminActor);

        verify(accessPolicy).enforceAdminBranchAccess(branch.getId(), adminActor);
        assertThat(response).isNotNull();
        assertThat(response.fullName()).isEqualTo("Dr. Mehmet Kaya");
        assertThat(response.staffType()).isEqualTo("DOCTOR");
        assertThat(response.specialty()).isEqualTo("Kardiyoloji");

        ArgumentCaptor<Membership> memCaptor = ArgumentCaptor.forClass(Membership.class);
        verify(membershipRepository).save(memCaptor.capture());
        assertThat(memCaptor.getValue().getRole()).isEqualTo(Role.ROLE_STAFF);
    }

    @Test
    @DisplayName("registerStaff: Fails when doctor is registered with blank specialty (TC-008)")
    void shouldFailWhenDoctorHasBlankSpecialty() {
        CreateStaffCommand cmd = new CreateStaffCommand(
                organization.getId(), branch.getId(), "doc@clinic.test", "StrongPass123",
                "Dr. Mehmet Kaya", "+905551112233", "DOCTOR", "  "
        );

        when(branchRepository.findById(branch.getId())).thenReturn(Optional.of(branch));
        when(userAccountRepository.findByEmail("doc@clinic.test")).thenReturn(Optional.of(staffUser));

        assertThatThrownBy(() -> service.registerStaff(cmd, adminActor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Specialty is required for staff type DOCTOR");
    }

    @Test
    @DisplayName("assignStaffToBranch: Successfully assigns staff to department within branch")
    void shouldAssignStaffToBranchAndDepartment() {
        AssignStaffCommand cmd = new AssignStaffCommand(staffProfile.getId(), branch.getId(), department.getId());

        when(staffProfileRepository.findById(staffProfile.getId())).thenReturn(Optional.of(staffProfile));
        when(branchRepository.findById(branch.getId())).thenReturn(Optional.of(branch));
        when(departmentRepository.findById(department.getId())).thenReturn(Optional.of(department));
        when(staffAssignmentRepository.existsByStaffIdAndBranchIdAndDepartmentId(
                staffProfile.getId(), branch.getId(), department.getId())).thenReturn(false);
        when(staffAssignmentRepository.save(any(StaffAssignment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(membershipRepository.findByUserAccountIdAndRole(staffUser.getId(), Role.ROLE_STAFF))
                .thenReturn(Collections.emptyList());

        StaffAssignmentResponse response = service.assignStaffToBranch(cmd, adminActor);

        verify(accessPolicy).enforceAdminBranchAccess(branch.getId(), adminActor);
        assertThat(response.staffId()).isEqualTo(staffProfile.getId());
        assertThat(response.branchId()).isEqualTo(branch.getId());
        assertThat(response.departmentId()).isEqualTo(department.getId());
        assertThat(response.active()).isTrue();
    }

    @Test
    @DisplayName("assignStaffToBranch: Fails when unauthorized admin attempts assignment")
    void shouldFailWhenUnauthorizedAdminAttemptsAssignment() {
        AssignStaffCommand cmd = new AssignStaffCommand(staffProfile.getId(), branch.getId(), department.getId());
        doThrow(new AccessDeniedException("Access denied")).when(accessPolicy).enforceAdminBranchAccess(branch.getId(), adminActor);

        assertThatThrownBy(() -> service.assignStaffToBranch(cmd, adminActor))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("configureEligibility: Successfully binds branch services to assignment")
    void shouldConfigureEligibility() {
        ServiceDefinition def = new ServiceDefinition(UUID.randomUUID(), organization, "Eko", "Eko Cekimi", true, Instant.now());
        BranchService bs = new BranchService(UUID.randomUUID(), organization, branch, def, 30, Money.of(new BigDecimal("1000.00"), Currency.getInstance("TRY")), true, Instant.now());

        ConfigureEligibilityCommand cmd = new ConfigureEligibilityCommand(assignment.getId(), List.of(bs.getId()));

        when(staffAssignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(branchServiceRepository.findById(bs.getId())).thenReturn(Optional.of(bs));
        when(staffServiceEligibilityRepository.save(any(StaffServiceEligibility.class))).thenAnswer(inv -> inv.getArgument(0));

        List<StaffServiceEligibilityResponse> responses = service.configureEligibility(cmd, adminActor);

        verify(accessPolicy).enforceAdminBranchAccess(branch.getId(), adminActor);
        verify(staffServiceEligibilityRepository).deleteByAssignmentId(assignment.getId());
        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().branchServiceId()).isEqualTo(bs.getId());
    }

    @Test
    @DisplayName("configureWeeklyAvailability: Detects overlapping windows on the same weekday")
    void shouldRejectOverlappingWeeklyAvailabilityWindows() {
        ConfigureWeeklyAvailabilityCommand cmd = new ConfigureWeeklyAvailabilityCommand(
                assignment.getId(),
                List.of(
                        new WeeklyAvailabilityItem((short) 1, LocalTime.of(9, 0), LocalTime.of(13, 0)),
                        new WeeklyAvailabilityItem((short) 1, LocalTime.of(12, 0), LocalTime.of(17, 0))
                )
        );

        when(staffAssignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> service.configureWeeklyAvailability(cmd, adminActor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Overlapping weekly availability windows");
    }

    @Test
    @DisplayName("addAvailabilityException: Successfully adds full day leave")
    void shouldAddFullDayLeave() {
        LocalDate leaveDate = LocalDate.of(2026, 11, 15);
        CreateAvailabilityExceptionCommand cmd = new CreateAvailabilityExceptionCommand(
                assignment.getId(), leaveDate, AvailabilityExceptionKind.FULL_DAY_LEAVE, null, null, "Kongre Katilimi"
        );

        when(staffAssignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(availabilityExceptionRepository.save(any(AvailabilityException.class))).thenAnswer(inv -> inv.getArgument(0));

        AvailabilityExceptionResponse response = service.addAvailabilityException(cmd, adminActor);

        assertThat(response.localDate()).isEqualTo(leaveDate);
        assertThat(response.kind()).isEqualTo(AvailabilityExceptionKind.FULL_DAY_LEAVE);
        assertThat(response.startTime()).isNull();
        assertThat(response.endTime()).isNull();
    }

    @Test
    @DisplayName("deleteWeeklyAvailability & deleteAvailabilityException verify ownership and prevent IDOR deletion")
    void deleteAvailabilityAndExceptionVerifyAssignmentOwnership() {
        Organization org = new Organization("Klinik");
        Branch b = new Branch(org, "Merkez", "Europe/Istanbul", null);
        Department d = new Department(b, "Kardiyoloji");
        UserAccount actor = new UserAccount("admin-del@clinic.test", "hash");
        UserAccount docUser = new UserAccount("doc-del@clinic.test", "hash");
        StaffProfile doc = new StaffProfile(docUser, "Dr. Selim", "555", StaffType.DOCTOR, "Kardiyoloji");
        StaffAssignment ownedAssignment = new StaffAssignment(doc, b, d);
        StaffAssignment foreignAssignment = new StaffAssignment(doc, b, d);

        WeeklyAvailability weekly = new WeeklyAvailability(ownedAssignment, (short) 1, LocalTime.of(9, 0), LocalTime.of(12, 0));
        AvailabilityException exc = AvailabilityException.fullDayLeave(ownedAssignment, LocalDate.now().plusDays(10), "İzin");

        when(securityContextFacade.getRequiredCurrentUser()).thenReturn(actor);
        when(staffAssignmentRepository.findById(ownedAssignment.getId())).thenReturn(Optional.of(ownedAssignment));
        when(weeklyAvailabilityRepository.findById(weekly.getId())).thenReturn(Optional.of(weekly));
        when(availabilityExceptionRepository.findById(exc.getId())).thenReturn(Optional.of(exc));

        service.deleteWeeklyAvailability(ownedAssignment.getId(), weekly.getId());
        verify(weeklyAvailabilityRepository).delete(weekly);

        service.deleteAvailabilityException(ownedAssignment.getId(), exc.getId());
        verify(availabilityExceptionRepository).delete(exc);

        // Attempting to delete owned record under a different assignmentId triggers AccessDeniedException
        when(staffAssignmentRepository.findById(foreignAssignment.getId())).thenReturn(Optional.of(foreignAssignment));
        assertThatThrownBy(() -> service.deleteWeeklyAvailability(foreignAssignment.getId(), weekly.getId()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.deleteAvailabilityException(foreignAssignment.getId(), exc.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("getAuthorizedStaffProfiles scopes staff directory strictly by admin authorized branch IDs")
    void getAuthorizedStaffProfilesScopesByAuthorizedBranches() {
        UserAccount adminUser = new UserAccount("scoped-admin@clinic.test", "hash");
        UUID authorizedBranchId = UUID.randomUUID();
        UserAccount docUser = new UserAccount("doc-scoped@clinic.test", "hash");
        StaffProfile scopedDoc = new StaffProfile(docUser, "Dr. Yetkili", "555", StaffType.DOCTOR, "Göz");

        when(securityContextFacade.getRequiredCurrentUser()).thenReturn(adminUser);
        when(accessPolicy.getAdminAuthorizedBranchIds(adminUser)).thenReturn(List.of(authorizedBranchId));
        when(staffProfileRepository.findAuthorizedStaffByBranchIds(List.of(authorizedBranchId)))
                .thenReturn(List.of(scopedDoc));

        List<StaffProfileResponse> result = service.getAuthorizedStaffProfiles();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).fullName()).isEqualTo("Dr. Yetkili");

        // Admin with zero authorized branches receives empty list cleanly without global query
        when(accessPolicy.getAdminAuthorizedBranchIds(adminUser)).thenReturn(List.of());
        assertThat(service.getAuthorizedStaffProfiles()).isEmpty();
    }
}
