package com.example.clinic.staff.application;

import com.example.clinic.AbstractIntegrationTest;
import com.example.clinic.catalog.application.CatalogManagementService;
import com.example.clinic.catalog.application.dto.AssignBranchServiceCommand;
import com.example.clinic.catalog.application.dto.BranchServiceResponse;
import com.example.clinic.catalog.application.dto.CreateServiceCommand;
import com.example.clinic.catalog.application.dto.ServiceDefinitionResponse;
import com.example.clinic.common.domain.TimeInterval;
import com.example.clinic.common.security.ClinicPrincipal;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import com.example.clinic.organization.application.OrganizationAdminService;
import com.example.clinic.organization.application.dto.BranchResponse;
import com.example.clinic.organization.application.dto.CreateBranchCommand;
import com.example.clinic.organization.application.dto.CreateDepartmentCommand;
import com.example.clinic.organization.application.dto.CreateOrganizationCommand;
import com.example.clinic.organization.application.dto.DepartmentResponse;
import com.example.clinic.organization.application.dto.OpeningHoursDto;
import com.example.clinic.organization.application.dto.OrganizationResponse;
import com.example.clinic.scheduling.application.AvailabilityCalculator;
import com.example.clinic.scheduling.application.port.AppointmentSchedulePort;
import com.example.clinic.scheduling.domain.TimeSlot;
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
import com.example.clinic.staff.domain.AvailabilityExceptionKind;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class StaffManagementServiceIT extends AbstractIntegrationTest {

    @Autowired
    private OrganizationAdminService organizationAdminService;

    @Autowired
    private CatalogManagementService catalogManagementService;

    @Autowired
    private StaffManagementService staffManagementService;

    @Autowired
    private AvailabilityCalculator availabilityCalculator;

    @Autowired
    private AppointmentSchedulePort appointmentSchedulePort;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private UserAccount testAdmin;

    @BeforeEach
    void setUpSecurityActor() {
        testAdmin = userAccountRepository.saveAndFlush(
                new UserAccount("staff_admin_" + UUID.randomUUID() + "@clinic.test", "hash123")
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
    @DisplayName("TC-007 / FR-007: Doktor ve teknisyen kaydet; staff_type ile ayrılır")
    void shouldRegisterStaffWithSpecialty() {
        OrganizationResponse org = organizationAdminService.createOrganization(
                new CreateOrganizationCommand("Memorial Sağlık Grubu")
        );
        BranchResponse branch = organizationAdminService.createBranch(
                org.id(), new CreateBranchCommand("Şişli Şubesi", "Europe/Istanbul", "Piyalepaşa Bulvarı")
        );

        // 1. Register DOCTOR
        StaffProfileResponse doctor = staffManagementService.registerStaff(new CreateStaffCommand(
                org.id(), branch.id(), "dr.ahmet." + UUID.randomUUID() + "@clinic.test", "Pass123456",
                "Dr. Ahmet Kaya", "+905551112233", "DOCTOR", "Kardiyoloji"
        ));

        // 2. Register TECHNICIAN
        StaffProfileResponse tech = staffManagementService.registerStaff(new CreateStaffCommand(
                org.id(), branch.id(), "tech.veli." + UUID.randomUUID() + "@clinic.test", "Pass123456",
                "Teknisyen Veli Can", "+905552223344", "TECHNICIAN", null
        ));

        assertThat(doctor.staffType()).isEqualTo("DOCTOR");
        assertThat(doctor.specialty()).isEqualTo("Kardiyoloji");
        assertThat(doctor.active()).isTrue();

        assertThat(tech.staffType()).isEqualTo("TECHNICIAN");
        assertThat(tech.specialty()).isNull();
        assertThat(tech.active()).isTrue();
    }

    @Test
    @DisplayName("TC-008 / FR-008: Doktora uzmanlık zorunludur; boş uzmanlıkla doktor kaydı reddedilir")
    void shouldRejectDoctorWithBlankSpecialty() {
        OrganizationResponse org = organizationAdminService.createOrganization(
                new CreateOrganizationCommand("Acıbadem Grubu")
        );
        BranchResponse branch = organizationAdminService.createBranch(
                org.id(), new CreateBranchCommand("Kadıköy Şubesi", "Europe/Istanbul", "Bağdat Cad.")
        );

        assertThatThrownBy(() -> staffManagementService.registerStaff(new CreateStaffCommand(
                org.id(), branch.id(), "dr.bos." + UUID.randomUUID() + "@clinic.test", "Pass123456",
                "Dr. Boş Uzmanlık", "+905551112233", "DOCTOR", "  "
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Specialty is required for staff type DOCTOR");
    }

    @Test
    @DisplayName("TC-007 / FR-007: Hekimi şubeye ve Kardiyoloji bölümüne atama (StaffAssignment)")
    void shouldAssignStaffToBranchAndDepartment() {
        OrganizationResponse org = organizationAdminService.createOrganization(
                new CreateOrganizationCommand("Florence Nightingale")
        );
        BranchResponse branch = organizationAdminService.createBranch(
                org.id(), new CreateBranchCommand("Ataşehir Şubesi", "Europe/Istanbul", "Brandium")
        );
        DepartmentResponse dept = organizationAdminService.createDepartment(
                branch.id(), new CreateDepartmentCommand("Kardiyoloji")
        );

        StaffProfileResponse doctor = staffManagementService.registerStaff(new CreateStaffCommand(
                org.id(), branch.id(), "dr.florence." + UUID.randomUUID() + "@clinic.test", "Pass123456",
                "Dr. Ayşe Yılmaz", "+905554445566", "DOCTOR", "Kardiyoloji"
        ));

        StaffAssignmentResponse assignment = staffManagementService.assignStaffToBranch(
                new AssignStaffCommand(doctor.id(), branch.id(), dept.id())
        );

        assertThat(assignment.id()).isNotNull();
        assertThat(assignment.staffId()).isEqualTo(doctor.id());
        assertThat(assignment.branchId()).isEqualTo(branch.id());
        assertThat(assignment.departmentId()).isEqualTo(dept.id());
        assertThat(assignment.active()).isTrue();

        List<StaffAssignmentResponse> branchAssignments = staffManagementService.getStaffAssignmentsByBranch(branch.id());
        assertThat(branchAssignments).hasSize(1);
        assertThat(branchAssignments.getFirst().staffName()).isEqualTo("Dr. Ayşe Yılmaz");
    }

    @Test
    @DisplayName("TC-009 / FR-009: Doktora Muayene hizmeti ver, verilmeyen Ultrason hizmeti yetkisiz kabul edilir")
    void shouldEnforceServiceEligibility() {
        OrganizationResponse org = organizationAdminService.createOrganization(
                new CreateOrganizationCommand("Medipol Mega")
        );
        BranchResponse branch = organizationAdminService.createBranch(
                org.id(), new CreateBranchCommand("Bağcılar Şubesi", "Europe/Istanbul", "TEM Çıkışı")
        );
        DepartmentResponse dept = organizationAdminService.createDepartment(
                branch.id(), new CreateDepartmentCommand("Kardiyoloji")
        );

        ServiceDefinitionResponse muayeneDef = catalogManagementService.defineService(
                org.id(), new CreateServiceCommand("Genel Muayene", "Rutin poliklinik")
        );
        ServiceDefinitionResponse ultrasonDef = catalogManagementService.defineService(
                org.id(), new CreateServiceCommand("Ekokardiyografi", "Eko Ultrason")
        );

        BranchServiceResponse muayeneService = catalogManagementService.assignServiceToBranch(
                branch.id(), new AssignBranchServiceCommand(muayeneDef.id(), 30, new BigDecimal("800.00"), "TRY")
        );
        BranchServiceResponse ultrasonService = catalogManagementService.assignServiceToBranch(
                branch.id(), new AssignBranchServiceCommand(ultrasonDef.id(), 45, new BigDecimal("1500.00"), "TRY")
        );

        StaffProfileResponse doctor = staffManagementService.registerStaff(new CreateStaffCommand(
                org.id(), branch.id(), "dr.medipol." + UUID.randomUUID() + "@clinic.test", "Pass123456",
                "Dr. Ali Demir", "+905559998877", "DOCTOR", "Kardiyoloji"
        ));

        StaffAssignmentResponse assignment = staffManagementService.assignStaffToBranch(
                new AssignStaffCommand(doctor.id(), branch.id(), dept.id())
        );

        // Grant eligibility ONLY to Muayene
        List<StaffServiceEligibilityResponse> eligibilities = staffManagementService.configureEligibility(
                new ConfigureEligibilityCommand(assignment.id(), List.of(muayeneService.id()))
        );

        assertThat(eligibilities).hasSize(1);
        assertThat(eligibilities.getFirst().branchServiceId()).isEqualTo(muayeneService.id());

        assertThat(availabilityCalculator.isStaffEligible(doctor.id(), muayeneService.id())).isTrue();
        assertThat(availabilityCalculator.isStaffEligible(doctor.id(), ultrasonService.id())).isFalse();
    }

    @Test
    @DisplayName("TC-010 & TC-011: Haftalık uygunluk, izin istisnası ve slot hesaplama tam entegrasyonu")
    void shouldConfigureWeeklyAvailabilityAndCalculateSlots() {
        OrganizationResponse org = organizationAdminService.createOrganization(
                new CreateOrganizationCommand("Liv Hospital")
        );
        BranchResponse branch = organizationAdminService.createBranch(
                org.id(), new CreateBranchCommand("Ulus Şubesi", "Europe/Istanbul", "Ahmet Adnan Saygun")
        );
        DepartmentResponse dept = organizationAdminService.createDepartment(
                branch.id(), new CreateDepartmentCommand("Kardiyoloji")
        );

        // Branch opening hours: Monday (1) 08:00 - 18:00
        organizationAdminService.configureOpeningHours(branch.id(), List.of(
                new OpeningHoursDto((short) 1, LocalTime.of(8, 0), LocalTime.of(18, 0))
        ));

        ServiceDefinitionResponse muayeneDef = catalogManagementService.defineService(
                org.id(), new CreateServiceCommand("Kardiyoloji Muayenesi", "Muayene")
        );
        BranchServiceResponse service = catalogManagementService.assignServiceToBranch(
                branch.id(), new AssignBranchServiceCommand(muayeneDef.id(), 30, new BigDecimal("1000.00"), "TRY")
        );

        StaffProfileResponse doctor = staffManagementService.registerStaff(new CreateStaffCommand(
                org.id(), branch.id(), "dr.liv." + UUID.randomUUID() + "@clinic.test", "Pass123456",
                "Dr. Canan Dağdeviren", "+905553332211", "DOCTOR", "Kardiyoloji"
        ));

        StaffAssignmentResponse assignment = staffManagementService.assignStaffToBranch(
                new AssignStaffCommand(doctor.id(), branch.id(), dept.id())
        );

        staffManagementService.configureEligibility(
                new ConfigureEligibilityCommand(assignment.id(), List.of(service.id()))
        );

        // Monday (1) 09:00 - 12:00
        List<WeeklyAvailabilityResponse> weekly = staffManagementService.configureWeeklyAvailability(
                new ConfigureWeeklyAvailabilityCommand(assignment.id(), List.of(
                        new WeeklyAvailabilityItem((short) 1, LocalTime.of(9, 0), LocalTime.of(12, 0))
                ))
        );
        assertThat(weekly).hasSize(1);
        assertThat(weekly.getFirst().weekday()).isEqualTo((short) 1);

        // Calculate available slots on Monday 2026-11-16 (future Monday)
        LocalDate nextMonday = LocalDate.of(2026, 11, 16);
        List<TimeSlot> slots = availabilityCalculator.calculateAvailableSlots(doctor.id(), service.id(), nextMonday);

        assertThat(slots).hasSize(6);
        assertThat(slots.stream().map(TimeSlot::startTime).toList()).containsExactly(
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                LocalTime.of(10, 0),
                LocalTime.of(10, 30),
                LocalTime.of(11, 0),
                LocalTime.of(11, 30)
        );

        // TC-011: Add partial leave between 10:00 and 11:00 on nextMonday
        AvailabilityExceptionResponse exception = staffManagementService.addAvailabilityException(
                new CreateAvailabilityExceptionCommand(
                        assignment.id(), nextMonday, AvailabilityExceptionKind.PARTIAL_LEAVE,
                        LocalTime.of(10, 0), LocalTime.of(11, 0), "Doktor Semineri"
                )
        );
        assertThat(exception.id()).isNotNull();

        List<TimeSlot> remainingSlots = availabilityCalculator.calculateAvailableSlots(doctor.id(), service.id(), nextMonday);
        assertThat(remainingSlots).hasSize(4);
        assertThat(remainingSlots.stream().map(TimeSlot::startTime).toList()).containsExactly(
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                LocalTime.of(11, 0),
                LocalTime.of(11, 30)
        );
    }
}
