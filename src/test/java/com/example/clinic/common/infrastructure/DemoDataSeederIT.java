package com.example.clinic.common.infrastructure;

import com.example.clinic.booking.application.AppointmentApprovalService;
import com.example.clinic.booking.application.BookingCommand;
import com.example.clinic.booking.application.BookingService;
import com.example.clinic.booking.application.PatientBookingQueryService;
import com.example.clinic.booking.application.dto.BranchSummaryDto;
import com.example.clinic.booking.application.dto.ServiceSummaryDto;
import com.example.clinic.booking.application.dto.StaffSummaryDto;
import com.example.clinic.booking.domain.Appointment;
import com.example.clinic.booking.domain.AppointmentStatus;
import com.example.clinic.booking.infrastructure.persistence.AppointmentJpaEntity;
import com.example.clinic.booking.infrastructure.persistence.AppointmentJpaRepository;
import com.example.clinic.booking.infrastructure.persistence.AppointmentStatusHistoryJpaEntity;
import com.example.clinic.booking.infrastructure.persistence.AppointmentStatusHistoryJpaRepository;
import com.example.clinic.catalog.domain.BranchService;
import com.example.clinic.catalog.domain.ServiceDefinition;
import com.example.clinic.catalog.infrastructure.BranchServiceRepository;
import com.example.clinic.catalog.infrastructure.ServiceDefinitionRepository;
import com.example.clinic.common.domain.Money;
import com.example.clinic.common.security.ClinicPrincipal;
import com.example.clinic.identity.application.ClinicUserDetailsService;
import com.example.clinic.identity.domain.Membership;
import com.example.clinic.identity.domain.PatientProfile;
import com.example.clinic.identity.domain.Role;
import com.example.clinic.identity.domain.StaffProfile;
import com.example.clinic.identity.domain.StaffType;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.MembershipRepository;
import com.example.clinic.identity.infrastructure.PatientProfileRepository;
import com.example.clinic.identity.infrastructure.StaffProfileRepository;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.Department;
import com.example.clinic.organization.domain.Organization;
import com.example.clinic.organization.infrastructure.BranchOpeningHoursRepository;
import com.example.clinic.organization.infrastructure.BranchRepository;
import com.example.clinic.organization.infrastructure.DepartmentRepository;
import com.example.clinic.organization.infrastructure.OrganizationRepository;
import com.example.clinic.scheduling.application.AvailabilityCalculator;
import com.example.clinic.scheduling.domain.TimeSlot;
import com.example.clinic.staff.domain.AvailabilityExceptionKind;
import com.example.clinic.staff.domain.StaffAssignment;
import com.example.clinic.staff.domain.StaffServiceEligibility;
import com.example.clinic.staff.domain.WeeklyAvailability;
import com.example.clinic.staff.infrastructure.AvailabilityExceptionRepository;
import com.example.clinic.staff.infrastructure.StaffAssignmentRepository;
import com.example.clinic.staff.infrastructure.StaffServiceEligibilityRepository;
import com.example.clinic.staff.infrastructure.WeeklyAvailabilityRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Full PostgreSQL integration suite verifying S09 demo seeding, idempotency,
 * environment isolation, partial seed recovery, scoped demo reset, and rollback safety (FR-034, NFR-005).
 * Uses an isolated PostgreSQL container so dev demo data never pollutes AbstractIntegrationTest's clean test DB.
 */
@SpringBootTest
@ActiveProfiles("dev")
class DemoDataSeederIT {

    private static final PostgreSQLContainer<?> DEMO_POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("clinic_demo_test_db")
            .withUsername("clinic_demo_user")
            .withPassword("clinic_demo_secret");

    static {
        DEMO_POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureDemoDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DEMO_POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", DEMO_POSTGRES::getUsername);
        registry.add("spring.datasource.password", DEMO_POSTGRES::getPassword);
    }

    @TestConfiguration
    static class ResetRollbackProbeConfig {
        static final AtomicBoolean FAIL_NEXT_RESET = new AtomicBoolean(false);

        @Bean
        ResetRollbackProbe resetRollbackProbe() {
            return new ResetRollbackProbe();
        }

        static class ResetRollbackProbe {
            @EventListener
            public void onResetCompleted(DemoDataResetCompletedEvent event) {
                if (FAIL_NEXT_RESET.compareAndSet(true, false)) {
                    throw new IllegalStateException("Simulated transactional failure during demo reset");
                }
            }
        }
    }

    @Autowired private DemoDataProperties demoProperties;
    @Autowired private DemoDataSeeder demoDataSeeder;
    @Autowired private DemoDataSeedService demoDataSeedService;
    @Autowired private DemoDataResetService demoDataResetService;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private BranchOpeningHoursRepository branchOpeningHoursRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private ServiceDefinitionRepository serviceDefinitionRepository;
    @Autowired private BranchServiceRepository branchServiceRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private PatientProfileRepository patientProfileRepository;
    @Autowired private StaffProfileRepository staffProfileRepository;
    @Autowired private StaffAssignmentRepository staffAssignmentRepository;
    @Autowired private StaffServiceEligibilityRepository staffServiceEligibilityRepository;
    @Autowired private WeeklyAvailabilityRepository weeklyAvailabilityRepository;
    @Autowired private AvailabilityExceptionRepository availabilityExceptionRepository;
    @Autowired private AppointmentJpaRepository appointmentJpaRepository;
    @Autowired private AppointmentStatusHistoryJpaRepository appointmentStatusHistoryJpaRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ClinicUserDetailsService clinicUserDetailsService;
    @Autowired private PatientBookingQueryService patientBookingQueryService;
    @Autowired private AvailabilityCalculator availabilityCalculator;
    @Autowired private BookingService bookingService;
    @Autowired private AppointmentApprovalService appointmentApprovalService;
    @Autowired private Clock clock;

    @AfterEach
    void restoreBaselineState() {
        SecurityContextHolder.clearContext();
        demoProperties.getReset().setEnabled(false);
        demoProperties.getReset().setReseed(true);
        ResetRollbackProbeConfig.FAIL_NEXT_RESET.set(false);
    }

    @Test
    @DisplayName("demoSeederPopulatesCompleteDataGraph: startup runner builds full tenant, staff, availability, and 5-status appointments")
    void demoSeederPopulatesCompleteDataGraph() {
        ensureSeededBaseline();

        Organization org = organizationRepository.findById(DemoDataCatalog.ORG_ID).orElseThrow();
        assertThat(org.getName()).isEqualTo("Örnek Sağlık Grubu");
        assertThat(org.isActive()).isTrue();

        assertThat(branchRepository.findByOrganizationId(org.getId()))
                .extracting(Branch::getName)
                .containsExactlyInAnyOrder("Merkez Hastanesi", "Sahil Polikliniği");

        assertThat(departmentRepository.findByBranchId(DemoDataCatalog.BRANCH_MERKEZ_ID))
                .extracting(Department::getName)
                .containsExactlyInAnyOrder("Kardiyoloji", "Radyoloji", "Dahiliye");
        assertThat(departmentRepository.findByBranchId(DemoDataCatalog.BRANCH_SAHIL_ID))
                .extracting(Department::getName)
                .containsExactlyInAnyOrder("Dahiliye", "Laboratuvar");

        assertThat(serviceDefinitionRepository.findByOrganizationId(org.getId()))
                .extracting(ServiceDefinition::getName)
                .containsExactlyInAnyOrder(
                        "Genel Muayene", "Eko Çekimi", "Radyoloji Akciğer Grafisi", "Kan Tahlili"
                );

        List<BranchService> merkezServices = branchServiceRepository.findByBranchId(DemoDataCatalog.BRANCH_MERKEZ_ID);
        assertThat(merkezServices).hasSize(3);
        assertThat(merkezServices).allSatisfy(bs -> assertThat(bs.getCurrency()).isEqualTo("TRY"));

        List<BranchService> sahilServices = branchServiceRepository.findByBranchId(DemoDataCatalog.BRANCH_SAHIL_ID);
        assertThat(sahilServices).hasSize(2);

        for (String email : DemoDataCatalog.ALL_ACCOUNT_EMAILS) {
            assertThat(userAccountRepository.findByEmail(email)).isPresent();
        }

        assertThat(staffAssignmentRepository.findByStaffId(DemoDataCatalog.STAFF_AHMET_ID)).hasSize(1);
        assertThat(staffAssignmentRepository.findByStaffId(DemoDataCatalog.STAFF_AYSE_ID)).hasSize(1);

        assertThat(staffServiceEligibilityRepository.findByAssignmentId(DemoDataCatalog.ASSIGNMENT_AHMET_MERKEZ_ID)).hasSize(3);
        assertThat(staffServiceEligibilityRepository.findByAssignmentId(DemoDataCatalog.ASSIGNMENT_AYSE_SAHIL_ID)).hasSize(2);

        assertThat(weeklyAvailabilityRepository.findByAssignmentId(DemoDataCatalog.ASSIGNMENT_AHMET_MERKEZ_ID)).hasSize(6);
        assertThat(weeklyAvailabilityRepository.findByAssignmentId(DemoDataCatalog.ASSIGNMENT_AYSE_SAHIL_ID)).hasSize(5);

        assertThat(availabilityExceptionRepository.findByAssignmentId(DemoDataCatalog.ASSIGNMENT_AHMET_MERKEZ_ID))
                .extracting(ex -> ex.getKind())
                .containsExactlyInAnyOrder(AvailabilityExceptionKind.PARTIAL_LEAVE, AvailabilityExceptionKind.FULL_DAY_LEAVE);
        assertThat(availabilityExceptionRepository.findByAssignmentId(DemoDataCatalog.ASSIGNMENT_AYSE_SAHIL_ID))
                .extracting(ex -> ex.getKind())
                .containsExactly(AvailabilityExceptionKind.OVERTIME);

        List<AppointmentJpaEntity> demoAppointments = DemoDataCatalog.ALL_APPOINTMENT_IDS.stream()
                .map(id -> appointmentJpaRepository.findById(id).orElseThrow())
                .toList();
        assertThat(demoAppointments).hasSize(7);
        assertThat(demoAppointments)
                .extracting(AppointmentJpaEntity::getStatus)
                .contains(
                        AppointmentStatus.PENDING,
                        AppointmentStatus.CONFIRMED,
                        AppointmentStatus.REJECTED,
                        AppointmentStatus.CANCELLED,
                        AppointmentStatus.COMPLETED
                );

        List<AppointmentStatusHistoryJpaEntity> completedHist =
                appointmentStatusHistoryJpaRepository.findByAppointmentIdOrderByOccurredAtAsc(DemoDataCatalog.APPT_COMPLETED_AHMET_ID);
        assertThat(completedHist).hasSize(3);
        assertThat(completedHist.get(0).getFromStatus()).isNull();
        assertThat(completedHist.get(0).getToStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(completedHist.get(1).getFromStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(completedHist.get(1).getToStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(completedHist.get(2).getFromStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(completedHist.get(2).getToStatus()).isEqualTo(AppointmentStatus.COMPLETED);

        List<AppointmentStatusHistoryJpaEntity> rejectedHist =
                appointmentStatusHistoryJpaRepository.findByAppointmentIdOrderByOccurredAtAsc(DemoDataCatalog.APPT_REJECTED_AYSE_ID);
        assertThat(rejectedHist).hasSize(2);
        assertThat(rejectedHist.get(1).getToStatus()).isEqualTo(AppointmentStatus.REJECTED);
        assertThat(rejectedHist.get(1).getReason()).isNotBlank();

        List<AppointmentStatusHistoryJpaEntity> cancelledHist =
                appointmentStatusHistoryJpaRepository.findByAppointmentIdOrderByOccurredAtAsc(DemoDataCatalog.APPT_CANCELLED_AHMET_ID);
        assertThat(cancelledHist).hasSize(3);
        assertThat(cancelledHist.get(2).getToStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(cancelledHist.get(2).getReason()).isNotBlank();
    }

    @Test
    @DisplayName("demoSeederIsIdempotent: repeated startup and service runs create zero duplicates")
    void demoSeederIsIdempotent() {
        ensureSeededBaseline();

        Map<String, Long> baselineCounts = snapshotEntityCounts();

        demoDataSeeder.run();
        demoDataSeedService.seedDemoData();
        demoDataSeeder.run();

        assertThat(snapshotEntityCounts()).isEqualTo(baselineCounts);
    }

    @Test
    @DisplayName("demoSeederHashesPasswords: accounts persist BCrypt hashes and authenticate via ClinicUserDetailsService")
    void demoSeederHashesPasswords() {
        ensureSeededBaseline();

        UserAccount admin = userAccountRepository.findByEmail(DemoDataCatalog.ADMIN_EMAIL).orElseThrow();
        UserAccount ahmet = userAccountRepository.findByEmail(DemoDataCatalog.DOCTOR_AHMET_EMAIL).orElseThrow();
        UserAccount ali = userAccountRepository.findByEmail(DemoDataCatalog.PATIENT_ALI_EMAIL).orElseThrow();

        assertThat(admin.getPasswordHash()).startsWith("$2").isNotEqualTo(demoProperties.getCredentials().getAdminPassword());
        assertThat(passwordEncoder.matches(demoProperties.getCredentials().getAdminPassword(), admin.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches(demoProperties.getCredentials().getDoctorPassword(), ahmet.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches(demoProperties.getCredentials().getPatientPassword(), ali.getPasswordHash())).isTrue();

        UserDetails adminDetails = clinicUserDetailsService.loadUserByUsername(DemoDataCatalog.ADMIN_EMAIL);
        assertThat(adminDetails.getAuthorities()).extracting(a -> a.getAuthority()).contains("ROLE_ADMIN");

        UserDetails doctorDetails = clinicUserDetailsService.loadUserByUsername(DemoDataCatalog.DOCTOR_AHMET_EMAIL);
        assertThat(doctorDetails.getAuthorities()).extracting(a -> a.getAuthority()).contains("ROLE_STAFF");

        UserDetails patientDetails = clinicUserDetailsService.loadUserByUsername(DemoDataCatalog.PATIENT_ALI_EMAIL);
        assertThat(patientDetails.getAuthorities()).extracting(a -> a.getAuthority()).contains("ROLE_PATIENT");
    }

    @Test
    @DisplayName("demoSeederPreservesTenantRelationships & Eligibility: query and availability engines resolve demo graph")
    void demoSeederPreservesTenantRelationshipsAndEligibility() {
        ensureSeededBaseline();

        assertThat(patientBookingQueryService.getActiveBranches())
                .extracting(BranchSummaryDto::id)
                .contains(DemoDataCatalog.BRANCH_MERKEZ_ID, DemoDataCatalog.BRANCH_SAHIL_ID);

        assertThat(patientBookingQueryService.getActiveServicesForBranch(DemoDataCatalog.BRANCH_SAHIL_ID))
                .extracting(ServiceSummaryDto::name)
                .containsExactlyInAnyOrder("Genel Muayene", "Kan Tahlili");

        assertThat(patientBookingQueryService.getEligibleStaffForService(DemoDataCatalog.BRANCH_SERVICE_MERKEZ_EKO_ID))
                .extracting(StaffSummaryDto::id)
                .containsExactly(DemoDataCatalog.STAFF_AHMET_ID);

        assertThat(patientBookingQueryService.getEligibleStaffForService(DemoDataCatalog.BRANCH_SERVICE_SAHIL_KAN_ID))
                .extracting(StaffSummaryDto::id)
                .containsExactly(DemoDataCatalog.STAFF_AYSE_ID);

        LocalDate nextWorkday = nextWorkdayAfterToday();
        List<TimeSlot> ahmetSlots = availabilityCalculator.calculateAvailableSlots(
                DemoDataCatalog.STAFF_AHMET_ID,
                DemoDataCatalog.BRANCH_SERVICE_MERKEZ_GENEL_ID,
                nextWorkday
        );
        // 10:00 (CONFIRMED) and 11:00..11:45 (PENDING Eko) occupy Ahmet, while 15:30 (CANCELLED) is freed
        assertThat(ahmetSlots).extracting(TimeSlot::startTime)
                .doesNotContain(LocalTime.of(10, 0), LocalTime.of(11, 0), LocalTime.of(11, 30))
                .contains(LocalTime.of(9, 0), LocalTime.of(15, 30));
    }

    @Test
    @DisplayName("demoSeederDoesNotOverwriteUnrelatedData: pre-existing non-demo tenant and user records remain untouched")
    void demoSeederDoesNotOverwriteUnrelatedData() {
        ensureSeededBaseline();
        NonDemoFixture external = createNonDemoTenant("Anadolu Tıp Merkezi");

        try {
            demoDataSeeder.run();

            Organization reloadedOrg = organizationRepository.findById(external.org().getId()).orElseThrow();
            assertThat(reloadedOrg.getName()).isEqualTo("Anadolu Tıp Merkezi");
            assertThat(branchRepository.findById(external.branch().getId())).isPresent();
            assertThat(userAccountRepository.findByEmail(external.patientUser().getEmail())).isPresent();
            assertThat(appointmentJpaRepository.findById(external.appointment().getId()))
                    .get()
                    .extracting(AppointmentJpaEntity::getStatus)
                    .isEqualTo(AppointmentStatus.CONFIRMED);
        } finally {
            deleteNonDemoFixture(external);
        }
    }

    @Test
    @DisplayName("demoSeederHandlesPartialExistingData: completes missing graph around pre-existing natural-key entities")
    void demoSeederHandlesPartialExistingData() {
        resetDemoDataNow();

        String customAdminHash = passwordEncoder.encode("CustomAdminPass9!");
        Organization customOrg = organizationRepository.saveAndFlush(new Organization(DemoDataCatalog.ORG_NAME));
        UserAccount customAdmin = userAccountRepository.saveAndFlush(new UserAccount(DemoDataCatalog.ADMIN_EMAIL, customAdminHash));
        UserAccount customAhmet = userAccountRepository.saveAndFlush(
                new UserAccount(DemoDataCatalog.DOCTOR_AHMET_EMAIL, passwordEncoder.encode("CustomDocPass9!"))
        );

        try {
            demoDataSeedService.seedDemoData();

            assertThat(organizationRepository.findByName(DemoDataCatalog.ORG_NAME))
                    .get().extracting(Organization::getId).isEqualTo(customOrg.getId());
            assertThat(userAccountRepository.findByEmail(DemoDataCatalog.ADMIN_EMAIL))
                    .get().satisfies(ua -> {
                        assertThat(ua.getId()).isEqualTo(customAdmin.getId());
                        assertThat(ua.getPasswordHash()).isEqualTo(customAdminHash);
                    });
            assertThat(userAccountRepository.findByEmail(DemoDataCatalog.DOCTOR_AHMET_EMAIL))
                    .get().extracting(UserAccount::getId).isEqualTo(customAhmet.getId());
            assertThat(branchRepository.findByOrganizationId(customOrg.getId())).hasSize(2);
            assertThat(staffProfileRepository.findByUserAccountId(customAhmet.getId())).isPresent();
            assertThat(appointmentJpaRepository.findAll()).hasSizeGreaterThanOrEqualTo(7);
        } finally {
            resetDemoDataNow();
            demoDataSeedService.seedDemoData();
        }
    }

    @Test
    @DisplayName("demoResetIsDisabledWithoutExplicitOptIn: reset rejects execution when clinic.demo.reset.enabled=false")
    void demoResetFailsClosedWhenOptInDisabled() {
        ensureSeededBaseline();
        demoProperties.getReset().setEnabled(false);

        assertThatThrownBy(() -> demoDataResetService.resetDemoData())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("clinic.demo.reset.enabled=true");

        assertThat(organizationRepository.findById(DemoDataCatalog.ORG_ID)).isPresent();
    }

    @Test
    @DisplayName("demoResetRemovesOnlyDemoRecords & PreservesNonDemoRecords: interactive demo data removed while non-demo tenant stays intact")
    void demoResetRemovesOnlyDemoRecordsAndPreservesNonDemoRecords() {
        ensureSeededBaseline();
        NonDemoFixture external = createNonDemoTenant("Ege Sağlık Grubu");

        try {
            // Also book an interactive appointment inside the demo tenant to ensure dynamic demo rows reset cleanly
            LocalDate interactiveDate = nextWorkdayAfterToday().plusDays(1);
            UserAccount aliUser = userAccountRepository.findById(DemoDataCatalog.USER_PATIENT_ALI_ID).orElseThrow();
            authenticateAsPatient(aliUser);
            Appointment interactiveAppt = bookingService.bookAppointment(new BookingCommand(
                    DemoDataCatalog.PATIENT_ALI_ID,
                    DemoDataCatalog.STAFF_AHMET_ID,
                    DemoDataCatalog.BRANCH_MERKEZ_ID,
                    DemoDataCatalog.BRANCH_SERVICE_MERKEZ_GENEL_ID,
                    interactiveDate,
                    LocalTime.of(9, 0)
            ));
            SecurityContextHolder.clearContext();
            assertThat(appointmentJpaRepository.findById(interactiveAppt.getId())).isPresent();

            resetDemoDataNow();

            assertThat(organizationRepository.findById(DemoDataCatalog.ORG_ID)).isEmpty();
            assertThat(branchRepository.findById(DemoDataCatalog.BRANCH_MERKEZ_ID)).isEmpty();
            assertThat(branchRepository.findById(DemoDataCatalog.BRANCH_SAHIL_ID)).isEmpty();
            assertThat(userAccountRepository.findByEmail(DemoDataCatalog.ADMIN_EMAIL)).isEmpty();
            assertThat(userAccountRepository.findByEmail(DemoDataCatalog.DOCTOR_AHMET_EMAIL)).isEmpty();
            assertThat(userAccountRepository.findByEmail(DemoDataCatalog.PATIENT_ALI_EMAIL)).isEmpty();
            assertThat(appointmentJpaRepository.findById(interactiveAppt.getId())).isEmpty();
            assertThat(appointmentStatusHistoryJpaRepository.findByAppointmentIdOrderByOccurredAtAsc(interactiveAppt.getId())).isEmpty();

            // Verify non-demo tenant survives completely intact
            assertThat(organizationRepository.findById(external.org().getId())).isPresent();
            assertThat(branchRepository.findById(external.branch().getId())).isPresent();
            assertThat(departmentRepository.findById(external.dept().getId())).isPresent();
            assertThat(serviceDefinitionRepository.findById(external.serviceDef().getId())).isPresent();
            assertThat(branchServiceRepository.findById(external.branchService().getId())).isPresent();
            assertThat(userAccountRepository.findById(external.staffUser().getId())).isPresent();
            assertThat(userAccountRepository.findById(external.patientUser().getId())).isPresent();
            assertThat(appointmentJpaRepository.findById(external.appointment().getId())).isPresent();
            assertThat(appointmentStatusHistoryJpaRepository.findByAppointmentIdOrderByOccurredAtAsc(external.appointment().getId())).hasSize(1);
        } finally {
            SecurityContextHolder.clearContext();
            deleteNonDemoFixture(external);
            ensureSeededBaseline();
        }
    }

    @Test
    @DisplayName("demoResetThenReseedRestoresDataGraph: runner resets and restores clean deterministic graph")
    void demoResetThenReseedRestoresDataGraph() {
        ensureSeededBaseline();
        try {
            demoProperties.getReset().setEnabled(true);
            demoProperties.getReset().setReseed(true);

            demoDataSeeder.run();

            assertThat(organizationRepository.findById(DemoDataCatalog.ORG_ID)).isPresent();
            assertThat(DemoDataCatalog.ALL_APPOINTMENT_IDS)
                    .allSatisfy(id -> assertThat(appointmentJpaRepository.findById(id)).isPresent());
        } finally {
            demoProperties.getReset().setEnabled(false);
        }
    }

    @Test
    @DisplayName("demoResetRollsBackOnFailure: simulated exception inside reset rolls back all PostgreSQL deletions")
    void demoResetRollsBackOnFailure() {
        ensureSeededBaseline();
        long preOrgCount = organizationRepository.count();
        long preUserCount = userAccountRepository.count();
        long preApptCount = appointmentJpaRepository.count();
        long preHistoryCount = appointmentStatusHistoryJpaRepository.count();

        try {
            demoProperties.getReset().setEnabled(true);
            ResetRollbackProbeConfig.FAIL_NEXT_RESET.set(true);

            assertThatThrownBy(() -> demoDataResetService.resetDemoData())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Simulated transactional failure");

            assertThat(organizationRepository.count()).isEqualTo(preOrgCount);
            assertThat(userAccountRepository.count()).isEqualTo(preUserCount);
            assertThat(appointmentJpaRepository.count()).isEqualTo(preApptCount);
            assertThat(appointmentStatusHistoryJpaRepository.count()).isEqualTo(preHistoryCount);
            assertThat(appointmentJpaRepository.findById(DemoDataCatalog.APPT_COMPLETED_AHMET_ID)).isPresent();
        } finally {
            ResetRollbackProbeConfig.FAIL_NEXT_RESET.set(false);
            demoProperties.getReset().setEnabled(false);
        }
    }

    @Test
    @DisplayName("Seeded demo accounts support end-to-end booking, approval, and cancellation lifecycle")
    void seededAccountsExecuteBookingApprovalAndCancellation() {
        ensureSeededBaseline();
        try {
            LocalDate freeDate = nextWorkdayAfterToday().plusDays(1);
            UserAccount aliUser = userAccountRepository.findById(DemoDataCatalog.USER_PATIENT_ALI_ID).orElseThrow();
            UserAccount ahmetUser = userAccountRepository.findById(DemoDataCatalog.USER_DOCTOR_AHMET_ID).orElseThrow();

            authenticateAsPatient(aliUser);
            Appointment booked = bookingService.bookAppointment(new BookingCommand(
                    DemoDataCatalog.PATIENT_ALI_ID,
                    DemoDataCatalog.STAFF_AHMET_ID,
                    DemoDataCatalog.BRANCH_MERKEZ_ID,
                    DemoDataCatalog.BRANCH_SERVICE_MERKEZ_GENEL_ID,
                    freeDate,
                    LocalTime.of(10, 0)
            ));
            SecurityContextHolder.clearContext();
            assertThat(booked.getStatus()).isEqualTo(AppointmentStatus.PENDING);

            appointmentApprovalService.approveAppointment(booked.getId(), ahmetUser.getId());
            assertThat(appointmentJpaRepository.findById(booked.getId()).orElseThrow().getStatus())
                    .isEqualTo(AppointmentStatus.CONFIRMED);

            appointmentApprovalService.cancelAppointment(booked.getId(), aliUser.getId(), "Plan değişikliği");
            assertThat(appointmentJpaRepository.findById(booked.getId()).orElseThrow().getStatus())
                    .isEqualTo(AppointmentStatus.CANCELLED);

            assertThat(appointmentStatusHistoryJpaRepository.findByAppointmentIdOrderByOccurredAtAsc(booked.getId()))
                    .hasSize(3);
        } finally {
            SecurityContextHolder.clearContext();
            resetDemoDataNow();
            demoDataSeedService.seedDemoData();
        }
    }

    private void ensureSeededBaseline() {
        demoDataSeedService.seedDemoData();
    }

    private void resetDemoDataNow() {
        demoProperties.getReset().setEnabled(true);
        try {
            demoDataResetService.resetDemoData();
        } finally {
            demoProperties.getReset().setEnabled(false);
        }
    }

    private Map<String, Long> snapshotEntityCounts() {
        return Map.ofEntries(
                Map.entry("org", organizationRepository.count()),
                Map.entry("branch", branchRepository.count()),
                Map.entry("openingHours", branchOpeningHoursRepository.count()),
                Map.entry("dept", departmentRepository.count()),
                Map.entry("serviceDef", serviceDefinitionRepository.count()),
                Map.entry("branchService", branchServiceRepository.count()),
                Map.entry("user", userAccountRepository.count()),
                Map.entry("membership", membershipRepository.count()),
                Map.entry("patient", patientProfileRepository.count()),
                Map.entry("staff", staffProfileRepository.count()),
                Map.entry("assignment", staffAssignmentRepository.count()),
                Map.entry("eligibility", staffServiceEligibilityRepository.count()),
                Map.entry("weeklyAvail", weeklyAvailabilityRepository.count()),
                Map.entry("exception", availabilityExceptionRepository.count()),
                Map.entry("appointment", appointmentJpaRepository.count()),
                Map.entry("history", appointmentStatusHistoryJpaRepository.count())
        );
    }

    private LocalDate nextWorkdayAfterToday() {
        LocalDate candidate = LocalDate.now(clock.withZone(ZoneId.of(DemoDataCatalog.BRANCH_ZONE_ISTANBUL))).plusDays(1);
        while (candidate.getDayOfWeek() == DayOfWeek.SATURDAY || candidate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }

    private void authenticateAsPatient(UserAccount patientUser) {
        ClinicPrincipal principal = new ClinicPrincipal(
                patientUser.getId(),
                patientUser.getEmail(),
                patientUser.getPasswordHash(),
                patientUser.isEnabled(),
                List.of(new SimpleGrantedAuthority(Role.ROLE_PATIENT.name()))
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private record NonDemoFixture(
            Organization org,
            Branch branch,
            Department dept,
            ServiceDefinition serviceDef,
            BranchService branchService,
            UserAccount staffUser,
            UserAccount patientUser,
            StaffProfile staff,
            PatientProfile patient,
            Membership staffMembership,
            Membership patientMembership,
            StaffAssignment assignment,
            StaffServiceEligibility eligibility,
            WeeklyAvailability weeklyAvailability,
            AppointmentJpaEntity appointment,
            AppointmentStatusHistoryJpaEntity history
    ) {
    }

    private NonDemoFixture createNonDemoTenant(String orgName) {
        Organization extOrg = organizationRepository.saveAndFlush(new Organization(orgName));
        Branch extBranch = branchRepository.saveAndFlush(new Branch(extOrg, "Harici Şube", "Europe/Istanbul", "İzmir"));
        Department extDept = departmentRepository.saveAndFlush(new Department(extBranch, "Ortopedi"));
        ServiceDefinition extDef = serviceDefinitionRepository.saveAndFlush(new ServiceDefinition(extOrg, "Harici Muayene", "Özel muayene"));
        BranchService extService = branchServiceRepository.saveAndFlush(
                new BranchService(extOrg, extBranch, extDef, 30, Money.of(new BigDecimal("2100.00"), Currency.getInstance("TRY")))
        );

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UserAccount staffUser = userAccountRepository.saveAndFlush(
                new UserAccount("ext.doc." + suffix + "@external.test", passwordEncoder.encode("External123!"))
        );
        UserAccount patientUser = userAccountRepository.saveAndFlush(
                new UserAccount("ext.pat." + suffix + "@external.test", passwordEncoder.encode("External123!"))
        );
        Membership staffMembership = membershipRepository.saveAndFlush(new Membership(staffUser, extOrg, extBranch, Role.ROLE_STAFF));
        Membership patientMembership = membershipRepository.saveAndFlush(new Membership(patientUser, extOrg, null, Role.ROLE_PATIENT));
        StaffProfile staff = staffProfileRepository.saveAndFlush(
                new StaffProfile(staffUser, "Dr. Harici Hekim", "+905559990001", StaffType.DOCTOR, "Ortopedi")
        );
        PatientProfile patient = patientProfileRepository.saveAndFlush(
                new PatientProfile(patientUser, "Harici Hasta", "+905559990002")
        );
        StaffAssignment assignment = staffAssignmentRepository.saveAndFlush(new StaffAssignment(staff, extBranch, extDept));
        StaffServiceEligibility eligibility = staffServiceEligibilityRepository.saveAndFlush(new StaffServiceEligibility(assignment, extService));
        WeeklyAvailability avail = weeklyAvailabilityRepository.saveAndFlush(
                new WeeklyAvailability(assignment, (short) 1, LocalTime.of(9, 0), LocalTime.of(17, 0))
        );

        Instant start = Instant.now().plusSeconds(86_400);
        Instant end = start.plusSeconds(1800);
        AppointmentJpaEntity appt = appointmentJpaRepository.saveAndFlush(new AppointmentJpaEntity(
                UUID.randomUUID(), patient.getId(), staff.getId(), extBranch.getId(), extService.getId(),
                start, end, AppointmentStatus.CONFIRMED, extDef.getName(), 30,
                new BigDecimal("2100.00"), "TRY", 0L, Instant.now()
        ));
        AppointmentStatusHistoryJpaEntity hist = appointmentStatusHistoryJpaRepository.saveAndFlush(new AppointmentStatusHistoryJpaEntity(
                UUID.randomUUID(), appt.getId(), null, AppointmentStatus.CONFIRMED,
                staffUser.getId(), "External tenant appointment", Instant.now()
        ));

        return new NonDemoFixture(
                extOrg, extBranch, extDept, extDef, extService,
                staffUser, patientUser, staff, patient,
                staffMembership, patientMembership, assignment, eligibility, avail, appt, hist
        );
    }

    private void deleteNonDemoFixture(NonDemoFixture f) {
        appointmentStatusHistoryJpaRepository.deleteAllByIdInBatch(Set.of(f.history().getId()));
        appointmentJpaRepository.deleteAllByIdInBatch(Set.of(f.appointment().getId()));
        weeklyAvailabilityRepository.deleteAllByIdInBatch(Set.of(f.weeklyAvailability().getId()));
        staffServiceEligibilityRepository.deleteAllByIdInBatch(Set.of(f.eligibility().getId()));
        staffAssignmentRepository.deleteAllByIdInBatch(Set.of(f.assignment().getId()));
        branchServiceRepository.deleteAllByIdInBatch(Set.of(f.branchService().getId()));
        serviceDefinitionRepository.deleteAllByIdInBatch(Set.of(f.serviceDef().getId()));
        departmentRepository.deleteAllByIdInBatch(Set.of(f.dept().getId()));
        membershipRepository.deleteAllByIdInBatch(Set.of(f.staffMembership().getId(), f.patientMembership().getId()));
        patientProfileRepository.deleteAllByIdInBatch(Set.of(f.patient().getId()));
        staffProfileRepository.deleteAllByIdInBatch(Set.of(f.staff().getId()));
        userAccountRepository.deleteAllByIdInBatch(Set.of(f.staffUser().getId(), f.patientUser().getId()));
        branchRepository.deleteAllByIdInBatch(Set.of(f.branch().getId()));
        organizationRepository.deleteAllByIdInBatch(Set.of(f.org().getId()));
    }
}
