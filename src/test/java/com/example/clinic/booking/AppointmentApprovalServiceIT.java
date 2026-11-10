package com.example.clinic.booking;

import com.example.clinic.AbstractIntegrationTest;
import com.example.clinic.booking.application.AppointmentApprovalService;
import com.example.clinic.booking.application.BookingCommand;
import com.example.clinic.booking.application.BookingService;
import com.example.clinic.booking.domain.AppointmentDomainException;
import com.example.clinic.booking.domain.AppointmentStatus;
import com.example.clinic.booking.infrastructure.adapter.DatabaseAppointmentSchedulePort;
import com.example.clinic.booking.infrastructure.persistence.AppointmentJpaEntity;
import com.example.clinic.booking.infrastructure.persistence.AppointmentJpaRepository;
import com.example.clinic.booking.infrastructure.persistence.AppointmentStatusHistoryJpaEntity;
import com.example.clinic.booking.infrastructure.persistence.AppointmentStatusHistoryJpaRepository;
import com.example.clinic.catalog.domain.BranchService;
import com.example.clinic.catalog.domain.ServiceDefinition;
import com.example.clinic.catalog.infrastructure.BranchServiceRepository;
import com.example.clinic.catalog.infrastructure.ServiceDefinitionRepository;
import com.example.clinic.common.domain.Money;
import com.example.clinic.common.domain.TimeInterval;
import com.example.clinic.common.security.SecurityContextFacade;
import com.example.clinic.identity.application.AccessPolicy;
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
import com.example.clinic.organization.domain.BranchOpeningHours;
import com.example.clinic.organization.domain.Department;
import com.example.clinic.organization.domain.Organization;
import com.example.clinic.organization.infrastructure.BranchOpeningHoursRepository;
import com.example.clinic.organization.infrastructure.BranchRepository;
import com.example.clinic.organization.infrastructure.DepartmentRepository;
import com.example.clinic.organization.infrastructure.OrganizationRepository;
import com.example.clinic.scheduling.application.AvailabilityCalculator;
import com.example.clinic.scheduling.domain.TimeSlot;
import com.example.clinic.staff.domain.StaffAssignment;
import com.example.clinic.staff.domain.StaffServiceEligibility;
import com.example.clinic.staff.domain.WeeklyAvailability;
import com.example.clinic.staff.infrastructure.StaffAssignmentRepository;
import com.example.clinic.staff.infrastructure.StaffServiceEligibilityRepository;
import com.example.clinic.staff.infrastructure.WeeklyAvailabilityRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

/**
 * Full PostgreSQL Testcontainers integration test suite validating Sprint S07.
 * Covers requirements FR-018, FR-019, FR-020, FR-021, FR-030 and test scenarios TC-018..TC-021, TC-030.
 */
class AppointmentApprovalServiceIT extends AbstractIntegrationTest {

    private static final Currency TRY = Currency.getInstance("TRY");

    @Autowired
    private AppointmentApprovalService approvalService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private AppointmentJpaRepository appointmentJpaRepository;

    @MockitoSpyBean
    private AppointmentStatusHistoryJpaRepository appointmentStatusHistoryJpaRepository;

    @Autowired
    private DatabaseAppointmentSchedulePort databaseAppointmentSchedulePort;

    @Autowired
    private AvailabilityCalculator availabilityCalculator;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private BranchOpeningHoursRepository branchOpeningHoursRepository;

    @Autowired
    private ServiceDefinitionRepository serviceDefinitionRepository;

    @Autowired
    private BranchServiceRepository branchServiceRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private StaffProfileRepository staffProfileRepository;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    @Autowired
    private StaffAssignmentRepository staffAssignmentRepository;

    @Autowired
    private StaffServiceEligibilityRepository staffServiceEligibilityRepository;

    @Autowired
    private WeeklyAvailabilityRepository weeklyAvailabilityRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private Clock clock;

    @Autowired
    private AccessPolicy accessPolicy;

    @Autowired
    private SecurityContextFacade securityContextFacade;

    private Organization org1;
    private Organization org2;
    private Branch branch1;
    private Branch branch2;
    private Department dept1;
    private ServiceDefinition serviceDef;
    private BranchService branchService1;

    private UserAccount doctorUser;
    private StaffProfile doctor;
    private UserAccount otherDoctorUser;
    private StaffProfile otherDoctor;

    private UserAccount patientUser;
    private PatientProfile patient;
    private UserAccount otherPatientUser;
    private PatientProfile otherPatient;

    private UserAccount branchManagerUser;
    private UserAccount otherOrgManagerUser;

    private LocalDate targetDate;
    private Instant appointmentStartUtc;
    private Instant appointmentEndUtc;

    @BeforeEach
    void setUp() {
        reset(appointmentStatusHistoryJpaRepository);

        // Find next Monday at least 2 weeks in future
        targetDate = LocalDate.now().plusWeeks(2);
        while (targetDate.getDayOfWeek().getValue() != 1) {
            targetDate = targetDate.plusDays(1);
        }

        // Target appointment times: Monday 10:00 to 10:30 UTC
        appointmentStartUtc = targetDate.atTime(10, 0).atZone(ZoneId.of("Europe/Istanbul")).toInstant();
        appointmentEndUtc = targetDate.atTime(10, 30).atZone(ZoneId.of("Europe/Istanbul")).toInstant();

        // Organizations & Branches
        org1 = organizationRepository.saveAndFlush(new Organization("S07 Medical Corp"));
        org2 = organizationRepository.saveAndFlush(new Organization("Other Healthcare Org"));

        branch1 = branchRepository.saveAndFlush(new Branch(org1, "Central Clinic", "Europe/Istanbul", "Main Plaza 1"));
        branch2 = branchRepository.saveAndFlush(new Branch(org2, "Foreign Clinic", "Europe/Istanbul", "Foreign St 2"));

        dept1 = departmentRepository.saveAndFlush(new Department(branch1, "Cardiology"));

        branchOpeningHoursRepository.saveAndFlush(
                new BranchOpeningHours(branch1, (short) 1, LocalTime.of(8, 0), LocalTime.of(19, 0))
        );

        serviceDef = serviceDefinitionRepository.saveAndFlush(new ServiceDefinition(org1, "Cardiology Exam", "Standard exam"));
        branchService1 = branchServiceRepository.saveAndFlush(
                new BranchService(org1, branch1, serviceDef, 30, Money.of(new BigDecimal("750.00"), TRY))
        );

        // Doctor
        doctorUser = userAccountRepository.saveAndFlush(new UserAccount("dr.s07." + UUID.randomUUID() + "@clinic.test", "hash123"));
        doctor = staffProfileRepository.saveAndFlush(new StaffProfile(doctorUser, "Dr. Ahmet Kaya", "+905551112233", StaffType.DOCTOR, "Cardiology"));
        membershipRepository.saveAndFlush(new Membership(doctorUser, org1, branch1, Role.ROLE_STAFF));

        StaffAssignment assignment = staffAssignmentRepository.saveAndFlush(new StaffAssignment(doctor, branch1, dept1));
        weeklyAvailabilityRepository.saveAndFlush(new WeeklyAvailability(assignment, (short) 1, LocalTime.of(9, 0), LocalTime.of(18, 0)));
        staffServiceEligibilityRepository.saveAndFlush(new StaffServiceEligibility(assignment, branchService1));

        // Other Doctor (assigned elsewhere)
        otherDoctorUser = userAccountRepository.saveAndFlush(new UserAccount("dr.other." + UUID.randomUUID() + "@clinic.test", "hash123"));
        otherDoctor = staffProfileRepository.saveAndFlush(new StaffProfile(otherDoctorUser, "Dr. Zeynep Bilgi", "+905552223344", StaffType.DOCTOR, "Neurology"));
        membershipRepository.saveAndFlush(new Membership(otherDoctorUser, org1, branch1, Role.ROLE_STAFF));

        // Patient
        patientUser = userAccountRepository.saveAndFlush(new UserAccount("pat.s07." + UUID.randomUUID() + "@clinic.test", "hash123"));
        patient = patientProfileRepository.saveAndFlush(new PatientProfile(patientUser, "Burak Demir", "+905559990011"));
        membershipRepository.saveAndFlush(new Membership(patientUser, org1, null, Role.ROLE_PATIENT));

        // Other Patient
        otherPatientUser = userAccountRepository.saveAndFlush(new UserAccount("pat.other." + UUID.randomUUID() + "@clinic.test", "hash123"));
        otherPatient = patientProfileRepository.saveAndFlush(new PatientProfile(otherPatientUser, "Selin Vural", "+905559990022"));
        membershipRepository.saveAndFlush(new Membership(otherPatientUser, org1, null, Role.ROLE_PATIENT));

        // Authorized Branch Manager (Admin for org1 / branch1)
        branchManagerUser = userAccountRepository.saveAndFlush(new UserAccount("mgr.s07." + UUID.randomUUID() + "@clinic.test", "hash123"));
        membershipRepository.saveAndFlush(new Membership(branchManagerUser, org1, branch1, Role.ROLE_ADMIN));

        // Outside-scope Manager (Admin for org2 only)
        otherOrgManagerUser = userAccountRepository.saveAndFlush(new UserAccount("mgr.other." + UUID.randomUUID() + "@clinic.test", "hash123"));
        membershipRepository.saveAndFlush(new Membership(otherOrgManagerUser, org2, branch2, Role.ROLE_ADMIN));
    }

    @AfterEach
    void tearDown() {
        reset(appointmentStatusHistoryJpaRepository);
        appointmentStatusHistoryJpaRepository.deleteAll();
        appointmentJpaRepository.deleteAll();
    }

    private AppointmentJpaEntity createPendingAppointmentFixture(Instant start, Instant end) {
        AppointmentJpaEntity entity = new AppointmentJpaEntity(
                UUID.randomUUID(),
                patient.getId(),
                doctor.getId(),
                branch1.getId(),
                branchService1.getId(),
                start,
                end,
                AppointmentStatus.PENDING,
                "Cardiology Exam",
                30,
                new BigDecimal("750.00"),
                "TRY",
                0L,
                Instant.now()
        );
        AppointmentJpaEntity saved = appointmentJpaRepository.saveAndFlush(entity);

        // Record initial creation audit event (TC-017)
        AppointmentStatusHistoryJpaEntity initialHistory = new AppointmentStatusHistoryJpaEntity(
                UUID.randomUUID(),
                saved.getId(),
                null,
                AppointmentStatus.PENDING,
                patientUser.getId(),
                null,
                Instant.now()
        );
        appointmentStatusHistoryJpaRepository.saveAndFlush(initialHistory);
        return saved;
    }

    private AppointmentApprovalService createServiceWithClock(Instant instant) {
        return new AppointmentApprovalService(
                appointmentJpaRepository,
                appointmentStatusHistoryJpaRepository,
                userAccountRepository,
                staffProfileRepository,
                patientProfileRepository,
                accessPolicy,
                securityContextFacade,
                Clock.fixed(instant, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("1. FR-018 / TC-018: Authorized doctor can approve pending appointment")
    void authorizedDoctorCanApprovePendingAppointment() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);

        approvalService.approveAppointment(fixture.getId(), doctorUser.getId());

        AppointmentJpaEntity updated = appointmentJpaRepository.findById(fixture.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);

        List<AppointmentStatusHistoryJpaEntity> history = appointmentStatusHistoryJpaRepository
                .findByAppointmentIdOrderByOccurredAtAsc(fixture.getId());
        assertThat(history).hasSize(2);
        assertThat(history.get(1).getFromStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(history.get(1).getToStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(history.get(1).getActorUserId()).isEqualTo(doctorUser.getId());
    }

    @Test
    @DisplayName("2. FR-019 / TC-019: Doctor rejects appointment with reason")
    void doctorRejectsAppointmentWithReason() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);

        approvalService.rejectAppointment(fixture.getId(), doctorUser.getId(), "Hekim acil vakaya çağrıldı");

        AppointmentJpaEntity updated = appointmentJpaRepository.findById(fixture.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AppointmentStatus.REJECTED);

        List<AppointmentStatusHistoryJpaEntity> history = appointmentStatusHistoryJpaRepository
                .findByAppointmentIdOrderByOccurredAtAsc(fixture.getId());
        assertThat(history).hasSize(2);
        assertThat(history.get(1).getFromStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(history.get(1).getToStatus()).isEqualTo(AppointmentStatus.REJECTED);
        assertThat(history.get(1).getReason()).isEqualTo("Hekim acil vakaya çağrıldı");
    }

    @Test
    @DisplayName("3. TC-019: Reject without reason or with blank reason should fail")
    void rejectWithoutReasonShouldFail() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);

        assertThatThrownBy(() -> approvalService.rejectAppointment(fixture.getId(), doctorUser.getId(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rejection reason must not be null or blank");

        assertThatThrownBy(() -> approvalService.rejectAppointment(fixture.getId(), doctorUser.getId(), "   \n\t  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rejection reason must not be null or blank");

        AppointmentJpaEntity unchanged = appointmentJpaRepository.findById(fixture.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    @DisplayName("4. Cannot approve already cancelled appointment")
    void cannotApproveAlreadyCancelledAppointment() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);
        approvalService.cancelAppointment(fixture.getId(), patientUser.getId(), "Vazgeçtim");

        assertThatThrownBy(() -> approvalService.approveAppointment(fixture.getId(), doctorUser.getId()))
                .isInstanceOf(AppointmentDomainException.class)
                .hasMessageContaining("INVALID_STATUS_TRANSITION");

        AppointmentJpaEntity reloaded = appointmentJpaRepository.findById(fixture.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("5. Security: Patient cannot approve own appointment")
    void patientCannotApproveOwnAppointment() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);

        assertThatThrownBy(() -> approvalService.approveAppointment(fixture.getId(), patientUser.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not authorized to approve");

        AppointmentJpaEntity unchanged = appointmentJpaRepository.findById(fixture.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    @DisplayName("6. FR-030 / TC-030: Status history records all transitions in order with secondary deterministic key")
    void statusHistoryRecordsAllTransitionsInOrder() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);

        // Transition 1: PENDING -> CONFIRMED (by manager before start)
        AppointmentApprovalService earlyService = createServiceWithClock(appointmentStartUtc.minus(Duration.ofMinutes(30)));
        earlyService.approveAppointment(fixture.getId(), branchManagerUser.getId());

        // Transition 2: CONFIRMED -> COMPLETED (by doctor after end)
        AppointmentApprovalService lateService = createServiceWithClock(appointmentEndUtc.plus(Duration.ofMinutes(10)));
        lateService.completeAppointment(fixture.getId(), doctorUser.getId());

        List<AppointmentStatusHistoryJpaEntity> history = lateService
                .getAppointmentStatusHistory(fixture.getId(), patientUser.getId());

        assertThat(history).hasSize(3);
        // Event 0: NULL -> PENDING (Creation)
        assertThat(history.get(0).getFromStatus()).isNull();
        assertThat(history.get(0).getToStatus()).isEqualTo(AppointmentStatus.PENDING);

        // Event 1: PENDING -> CONFIRMED (Manager approval)
        assertThat(history.get(1).getFromStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(history.get(1).getToStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(history.get(1).getActorUserId()).isEqualTo(branchManagerUser.getId());

        // Event 2: CONFIRMED -> COMPLETED (Doctor completion)
        assertThat(history.get(2).getFromStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(history.get(2).getToStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(history.get(2).getActorUserId()).isEqualTo(doctorUser.getId());
    }

    @Test
    @DisplayName("7. Manager approves within authorized scope")
    void managerApprovesWithinAuthorizedScope() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);

        approvalService.approveAppointment(fixture.getId(), branchManagerUser.getId());

        AppointmentJpaEntity updated = appointmentJpaRepository.findById(fixture.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    @DisplayName("8. Manager rejects with reason within authorized scope")
    void managerRejectsWithReason() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);

        approvalService.rejectAppointment(fixture.getId(), branchManagerUser.getId(), "Tadilat nedeniyle kapalı");

        AppointmentJpaEntity updated = appointmentJpaRepository.findById(fixture.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AppointmentStatus.REJECTED);
    }

    @Test
    @DisplayName("9. Security: Manager denied outside organization/branch scope")
    void managerDeniedOutsideOrganizationScope() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);

        assertThatThrownBy(() -> approvalService.approveAppointment(fixture.getId(), otherOrgManagerUser.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not authorized to approve");

        AppointmentJpaEntity unchanged = appointmentJpaRepository.findById(fixture.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    @DisplayName("10. Security: Doctor denied outside assignment (cannot approve other doctor's appointment)")
    void doctorDeniedOutsideAssignment() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);

        assertThatThrownBy(() -> approvalService.approveAppointment(fixture.getId(), otherDoctorUser.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not authorized to approve");

        AppointmentJpaEntity unchanged = appointmentJpaRepository.findById(fixture.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    @DisplayName("11. FR-020 / TC-020: Patient cancels own pending appointment")
    void patientCancelsOwnPendingAppointment() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);

        approvalService.cancelAppointment(fixture.getId(), patientUser.getId(), "İşim çıktı");

        AppointmentJpaEntity updated = appointmentJpaRepository.findById(fixture.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("12. FR-020: Patient cancels own confirmed appointment")
    void patientCancelsOwnConfirmedAppointment() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);
        approvalService.approveAppointment(fixture.getId(), doctorUser.getId());

        approvalService.cancelAppointment(fixture.getId(), patientUser.getId(), "Hasta rahatsızlandı");

        AppointmentJpaEntity updated = appointmentJpaRepository.findById(fixture.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("13. Security: Patient denied when cancelling another patient's appointment")
    void patientDeniedWhenCancellingAnotherPatientsAppointment() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);

        assertThatThrownBy(() -> approvalService.cancelAppointment(fixture.getId(), otherPatientUser.getId(), "Yetkisiz iptal"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not authorized to cancel");

        AppointmentJpaEntity unchanged = appointmentJpaRepository.findById(fixture.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    @DisplayName("14. Doctor cancels eligible appointment")
    void doctorCancelsEligibleAppointment() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);

        approvalService.cancelAppointment(fixture.getId(), doctorUser.getId(), "Hekim acil ameliyatta");

        AppointmentJpaEntity updated = appointmentJpaRepository.findById(fixture.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("15. Temporal: Cancellation at appointment start is rejected")
    void cancellationAtAppointmentStartIsRejected() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);
        AppointmentApprovalService serviceAtStart = createServiceWithClock(appointmentStartUtc);

        assertThatThrownBy(() -> serviceAtStart.cancelAppointment(fixture.getId(), patientUser.getId(), "Geç kaldım"))
                .isInstanceOf(AppointmentDomainException.class)
                .hasMessageContaining("APPOINTMENT_ALREADY_STARTED");

        AppointmentJpaEntity unchanged = appointmentJpaRepository.findById(fixture.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    @DisplayName("16. Temporal: Cancellation immediately before start succeeds")
    void cancellationImmediatelyBeforeStartSucceeds() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);
        AppointmentApprovalService serviceBeforeStart = createServiceWithClock(appointmentStartUtc.minusMillis(10));

        serviceBeforeStart.cancelAppointment(fixture.getId(), patientUser.getId(), "Son dakika randevu iptali");

        AppointmentJpaEntity updated = appointmentJpaRepository.findById(fixture.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("17. FR-021 / TC-021: Doctor completes after appointment end")
    void doctorCompletesAfterAppointmentEnd() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);
        fixture.setStatus(AppointmentStatus.CONFIRMED);
        appointmentJpaRepository.saveAndFlush(fixture);

        AppointmentApprovalService serviceAfterEnd = createServiceWithClock(appointmentEndUtc.plus(Duration.ofMinutes(5)));
        serviceAfterEnd.completeAppointment(fixture.getId(), doctorUser.getId());

        AppointmentJpaEntity updated = appointmentJpaRepository.findById(fixture.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    @DisplayName("18. Temporal: Completion before end is rejected")
    void completionBeforeEndIsRejected() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);
        fixture.setStatus(AppointmentStatus.CONFIRMED);
        appointmentJpaRepository.saveAndFlush(fixture);

        AppointmentApprovalService serviceBeforeEnd = createServiceWithClock(appointmentEndUtc.minus(Duration.ofMinutes(5)));
        assertThatThrownBy(() -> serviceBeforeEnd.completeAppointment(fixture.getId(), doctorUser.getId()))
                .isInstanceOf(AppointmentDomainException.class)
                .hasMessageContaining("APPOINTMENT_NOT_ENDED");

        AppointmentJpaEntity unchanged = appointmentJpaRepository.findById(fixture.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    @DisplayName("19. Temporal: Completion at the exact end boundary succeeds")
    void completionAtTheExactEndBoundarySucceeds() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);
        fixture.setStatus(AppointmentStatus.CONFIRMED);
        appointmentJpaRepository.saveAndFlush(fixture);

        AppointmentApprovalService serviceAtEnd = createServiceWithClock(appointmentEndUtc);
        serviceAtEnd.completeAppointment(fixture.getId(), doctorUser.getId());

        AppointmentJpaEntity updated = appointmentJpaRepository.findById(fixture.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    @DisplayName("20. Security: Manager cannot complete doctor-only appointment")
    void managerCannotCompleteDoctorOnlyAppointment() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);
        fixture.setStatus(AppointmentStatus.CONFIRMED);
        appointmentJpaRepository.saveAndFlush(fixture);

        AppointmentApprovalService serviceAfterEnd = createServiceWithClock(appointmentEndUtc.plus(Duration.ofMinutes(10)));
        assertThatThrownBy(() -> serviceAfterEnd.completeAppointment(fixture.getId(), branchManagerUser.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("only the assigned doctor can complete");

        AppointmentJpaEntity unchanged = appointmentJpaRepository.findById(fixture.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    @DisplayName("21. Invariant: Invalid terminal-state transitions are rejected")
    void invalidTerminalStateTransitionsAreRejected() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);
        approvalService.rejectAppointment(fixture.getId(), doctorUser.getId(), "Reddedildi");

        // REJECTED -> CONFIRMED fails
        assertThatThrownBy(() -> approvalService.approveAppointment(fixture.getId(), doctorUser.getId()))
                .isInstanceOf(AppointmentDomainException.class)
                .hasMessageContaining("INVALID_STATUS_TRANSITION");

        // REJECTED -> CANCELLED fails
        assertThatThrownBy(() -> approvalService.cancelAppointment(fixture.getId(), patientUser.getId(), "İptal"))
                .isInstanceOf(AppointmentDomainException.class)
                .hasMessageContaining("INVALID_STATUS_TRANSITION");

        // REJECTED -> COMPLETED fails
        assertThatThrownBy(() -> approvalService.completeAppointment(fixture.getId(), doctorUser.getId()))
                .isInstanceOf(AppointmentDomainException.class)
                .hasMessageContaining("INVALID_STATUS_TRANSITION");
    }

    @Test
    @DisplayName("22. Idempotency: Repeated approval cannot append duplicate history")
    void repeatedApprovalCannotAppendDuplicateHistory() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);
        approvalService.approveAppointment(fixture.getId(), doctorUser.getId());

        // Second approval must fail and produce zero additional history records
        assertThatThrownBy(() -> approvalService.approveAppointment(fixture.getId(), doctorUser.getId()))
                .isInstanceOf(AppointmentDomainException.class)
                .hasMessageContaining("INVALID_STATUS_TRANSITION");

        List<AppointmentStatusHistoryJpaEntity> history = appointmentStatusHistoryJpaRepository
                .findByAppointmentIdOrderByOccurredAtAsc(fixture.getId());
        // Exactly 2 records: 1 creation + 1 approval
        assertThat(history).hasSize(2);
    }

    @Test
    @DisplayName("23. Validation: Blank and whitespace-only rejection reasons fail")
    void blankAndWhitespaceOnlyRejectionReasonsFail() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);

        assertThatThrownBy(() -> approvalService.rejectAppointment(fixture.getId(), doctorUser.getId(), "    "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rejection reason must not be null or blank");

        assertThatThrownBy(() -> approvalService.cancelAppointment(fixture.getId(), patientUser.getId(), "\t\n "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cancellation reason must not be null or blank");
    }

    @Test
    @DisplayName("24. Validation: Excessively long reasons fail")
    void excessivelyLongReasonsFail() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);
        String oversized = "R".repeat(501);

        assertThatThrownBy(() -> approvalService.rejectAppointment(fixture.getId(), doctorUser.getId(), oversized))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not exceed 500 characters");

        assertThatThrownBy(() -> approvalService.cancelAppointment(fixture.getId(), patientUser.getId(), oversized))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not exceed 500 characters");
    }

    @Test
    @DisplayName("25. Validation: Invalid or missing appointment IDs fail consistently")
    void invalidOrMissingAppointmentIdsFailConsistently() {
        assertThatThrownBy(() -> approvalService.approveAppointment(null, doctorUser.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Appointment ID must not be null");

        UUID nonExistentId = UUID.randomUUID();
        assertThatThrownBy(() -> approvalService.approveAppointment(nonExistentId, doctorUser.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Appointment not found");
    }

    @Test
    @DisplayName("26. Atomicity: History and state roll back together on persistence failure (Failure Injection)")
    void historyAndStateRollBackTogetherOnPersistenceFailure() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);

        // Inject simulated database failure during history append
        doThrow(new RuntimeException("Simulated disk error during audit write"))
                .when(appointmentStatusHistoryJpaRepository).save(any(AppointmentStatusHistoryJpaEntity.class));

        assertThatThrownBy(() -> approvalService.approveAppointment(fixture.getId(), doctorUser.getId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Simulated disk error");

        // Verify transaction rolled back: status remains PENDING in database
        AppointmentJpaEntity reloaded = appointmentJpaRepository.findById(fixture.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(AppointmentStatus.PENDING);

        // Reset spy for subsequent tests
        reset(appointmentStatusHistoryJpaRepository);

        // Verify audit history was not committed
        List<AppointmentStatusHistoryJpaEntity> history = appointmentStatusHistoryJpaRepository
                .findByAppointmentIdOrderByOccurredAtAsc(fixture.getId());
        assertThat(history).hasSize(1); // Only initial creation record
    }

    @Test
    @DisplayName("27. ADR-003: Rejected appointment releases staff availability")
    void rejectedAppointmentReleasesStaffAvailability() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);

        // Before rejection, occupied intervals contains the slot
        List<TimeInterval> occupiedBefore = databaseAppointmentSchedulePort
                .findOccupiedIntervals(doctor.getId(), targetDate, ZoneId.of("Europe/Istanbul"));
        assertThat(occupiedBefore).isNotEmpty();

        // Reject appointment
        approvalService.rejectAppointment(fixture.getId(), doctorUser.getId(), "Slot serbest bırakılıyor");

        // After rejection, occupied intervals NO LONGER contains the slot
        List<TimeInterval> occupiedAfter = databaseAppointmentSchedulePort
                .findOccupiedIntervals(doctor.getId(), targetDate, ZoneId.of("Europe/Istanbul"));
        assertThat(occupiedAfter).isEmpty();

        // Slot becomes visible in AvailabilityCalculator
        List<TimeSlot> availableSlots = availabilityCalculator
                .calculateAvailableSlots(doctor.getId(), branchService1.getId(), targetDate);
        assertThat(availableSlots).anyMatch(slot ->
                slot.startTime().equals(LocalTime.of(10, 0)) && slot.endTime().equals(LocalTime.of(10, 30))
        );
    }

    @Test
    @DisplayName("28. ADR-003: Cancelled appointment releases staff availability")
    void cancelledAppointmentReleasesStaffAvailability() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);

        // Cancel appointment
        approvalService.cancelAppointment(fixture.getId(), patientUser.getId(), "Hasta iptal etti");

        List<TimeInterval> occupied = databaseAppointmentSchedulePort
                .findOccupiedIntervals(doctor.getId(), targetDate, ZoneId.of("Europe/Istanbul"));
        assertThat(occupied).isEmpty();

        List<TimeSlot> availableSlots = availabilityCalculator
                .calculateAvailableSlots(doctor.getId(), branchService1.getId(), targetDate);
        assertThat(availableSlots).anyMatch(slot ->
                slot.startTime().equals(LocalTime.of(10, 0)) && slot.endTime().equals(LocalTime.of(10, 30))
        );
    }

    @Test
    @DisplayName("29. ADR-003: Confirmed appointment continues blocking availability")
    void confirmedAppointmentContinuesBlockingAvailability() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);
        approvalService.approveAppointment(fixture.getId(), doctorUser.getId());

        List<TimeInterval> occupied = databaseAppointmentSchedulePort
                .findOccupiedIntervals(doctor.getId(), targetDate, ZoneId.of("Europe/Istanbul"));
        assertThat(occupied).hasSize(1);

        List<TimeSlot> availableSlots = availabilityCalculator
                .calculateAvailableSlots(doctor.getId(), branchService1.getId(), targetDate);
        assertThat(availableSlots).noneMatch(slot ->
                slot.startTime().equals(LocalTime.of(10, 0)) && slot.endTime().equals(LocalTime.of(10, 30))
        );
    }

    @Test
    @DisplayName("30. ADR-003: Completed appointment remains non-rebookable in its historical interval")
    void completedAppointmentRemainsNonRebookableInItsHistoricalInterval() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);
        fixture.setStatus(AppointmentStatus.CONFIRMED);
        appointmentJpaRepository.saveAndFlush(fixture);

        AppointmentApprovalService serviceAfterEnd = createServiceWithClock(appointmentEndUtc.plus(Duration.ofMinutes(5)));
        serviceAfterEnd.completeAppointment(fixture.getId(), doctorUser.getId());

        // Historical slot remains blocking in port
        List<TimeInterval> occupied = databaseAppointmentSchedulePort
                .findOccupiedIntervals(doctor.getId(), targetDate, ZoneId.of("Europe/Istanbul"));
        assertThat(occupied).anyMatch(interval -> interval.startAt().equals(appointmentStartUtc) && interval.endAt().equals(appointmentEndUtc));
    }

    @Test
    @DisplayName("31. S06 Compatibility: Booking concurrency protections remain intact after slot release")
    void existingS06BookingConcurrencyProtectionsRemainIntact() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);
        approvalService.cancelAppointment(fixture.getId(), patientUser.getId(), "Slot yeniden açılsın");

        // The slot is now released. Rebooking the released slot by other patient succeeds
        BookingCommand cmd = new BookingCommand(
                otherPatient.getId(),
                doctor.getId(),
                branch1.getId(),
                branchService1.getId(),
                targetDate,
                LocalTime.of(10, 0),
                null
        );

        // Simulating second booking inside a transaction
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // Verify overlapping queries return no blocking appointments
        List<AppointmentJpaEntity> overlaps = appointmentJpaRepository.findOverlappingAppointmentsForStaff(
                doctor.getId(),
                appointmentStartUtc,
                appointmentEndUtc,
                List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED)
        );
        assertThat(overlaps).isEmpty();
    }

    @Test
    @DisplayName("32. Security: Unauthorized transitions never mutate state or history")
    void unauthorizedTransitionsNeverMutateStateOrHistory() {
        AppointmentJpaEntity fixture = createPendingAppointmentFixture(appointmentStartUtc, appointmentEndUtc);

        assertThatThrownBy(() -> approvalService.approveAppointment(fixture.getId(), otherPatientUser.getId()))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> approvalService.rejectAppointment(fixture.getId(), otherPatientUser.getId(), "Yetkisiz"))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> approvalService.cancelAppointment(fixture.getId(), otherPatientUser.getId(), "Yetkisiz"))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> approvalService.completeAppointment(fixture.getId(), otherPatientUser.getId()))
                .isInstanceOf(AccessDeniedException.class);

        // Appointment remains PENDING
        AppointmentJpaEntity reloaded = appointmentJpaRepository.findById(fixture.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(AppointmentStatus.PENDING);

        // History count unchanged (only 1 initial creation record)
        List<AppointmentStatusHistoryJpaEntity> history = appointmentStatusHistoryJpaRepository
                .findByAppointmentIdOrderByOccurredAtAsc(fixture.getId());
        assertThat(history).hasSize(1);
    }
}
