package com.example.clinic.booking;

import com.example.clinic.AbstractIntegrationTest;
import com.example.clinic.booking.application.AppointmentConflictException;
import com.example.clinic.booking.application.BookingCommand;
import com.example.clinic.booking.application.BookingService;
import com.example.clinic.booking.application.BookingValidationException;
import com.example.clinic.booking.application.PatientOverlapException;
import com.example.clinic.booking.domain.Appointment;
import com.example.clinic.booking.domain.AppointmentStatus;
import com.example.clinic.booking.domain.ServiceSnapshot;
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
import com.example.clinic.identity.domain.PatientProfile;
import com.example.clinic.identity.domain.Role;
import com.example.clinic.identity.domain.StaffProfile;
import com.example.clinic.identity.domain.StaffType;
import com.example.clinic.identity.domain.UserAccount;
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
import com.example.clinic.staff.domain.AvailabilityException;
import com.example.clinic.staff.domain.AvailabilityExceptionKind;
import com.example.clinic.staff.domain.StaffAssignment;
import com.example.clinic.staff.domain.StaffServiceEligibility;
import com.example.clinic.staff.domain.WeeklyAvailability;
import com.example.clinic.staff.infrastructure.AvailabilityExceptionRepository;
import com.example.clinic.staff.infrastructure.StaffAssignmentRepository;
import com.example.clinic.staff.infrastructure.StaffServiceEligibilityRepository;
import com.example.clinic.staff.infrastructure.WeeklyAvailabilityRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Functional integration tests for {@link BookingService}.
 * Verifies business requirements FR-012..FR-017, FR-029, FR-032, and FR-033.
 */
class BookingServiceIT extends AbstractIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private AppointmentJpaRepository appointmentJpaRepository;

    @Autowired
    private AppointmentStatusHistoryJpaRepository appointmentStatusHistoryJpaRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private BranchOpeningHoursRepository branchOpeningHoursRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    @Autowired
    private StaffProfileRepository staffProfileRepository;

    @Autowired
    private StaffAssignmentRepository staffAssignmentRepository;

    @Autowired
    private StaffServiceEligibilityRepository staffServiceEligibilityRepository;

    @Autowired
    private WeeklyAvailabilityRepository weeklyAvailabilityRepository;

    @Autowired
    private AvailabilityExceptionRepository availabilityExceptionRepository;

    @Autowired
    private ServiceDefinitionRepository serviceDefinitionRepository;

    @Autowired
    private BranchServiceRepository branchServiceRepository;

    private Organization org;
    private Branch branch;
    private Branch dstBranch;
    private Department dept;
    private ServiceDefinition serviceDef;
    private BranchService branchService;
    private StaffProfile doctor;
    private StaffAssignment assignment;
    private PatientProfile patient;
    private PatientProfile otherPatient;
    private UserAccount patientUser;
    private UserAccount otherPatientUser;
    private LocalDate targetDate;

    @BeforeEach
    void setUp() {
        // Find next Monday at least 2 weeks in the future
        targetDate = LocalDate.now().plusWeeks(2);
        while (targetDate.getDayOfWeek().getValue() != 1) {
            targetDate = targetDate.plusDays(1);
        }

        org = organizationRepository.saveAndFlush(new Organization("Metropolitan Medical"));
        branch = branchRepository.saveAndFlush(new Branch(org, "Central Clinic", "Europe/Istanbul", "Main Plaza 10"));
        dstBranch = branchRepository.saveAndFlush(new Branch(org, "Berlin Clinic", "Europe/Berlin", "Unter den Linden 5"));

        dept = departmentRepository.saveAndFlush(new Department(branch, "Internal Medicine"));

        // Branch hours: Monday (1) 09:00 - 18:00
        branchOpeningHoursRepository.saveAndFlush(new BranchOpeningHours(branch, (short) 1, LocalTime.of(9, 0), LocalTime.of(18, 0)));
        branchOpeningHoursRepository.saveAndFlush(new BranchOpeningHours(dstBranch, (short) 7, LocalTime.of(2, 0), LocalTime.of(18, 0))); // For Sunday DST tests

        serviceDef = serviceDefinitionRepository.saveAndFlush(new ServiceDefinition(org, "Check-Up", "General Check-Up"));
        branchService = branchServiceRepository.saveAndFlush(
                new BranchService(org, branch, serviceDef, 30, Money.of(new BigDecimal("600.00"), Currency.getInstance("TRY")))
        );

        UserAccount docUser = userAccountRepository.saveAndFlush(new UserAccount("doc." + UUID.randomUUID() + "@clinic.test", "hash123"));
        doctor = staffProfileRepository.saveAndFlush(new StaffProfile(docUser, "Dr. Mehmet Oz", "+905559998877", StaffType.DOCTOR, "Internal Medicine"));

        assignment = staffAssignmentRepository.saveAndFlush(new StaffAssignment(doctor, branch, dept));
        weeklyAvailabilityRepository.saveAndFlush(new WeeklyAvailability(assignment, (short) 1, LocalTime.of(9, 0), LocalTime.of(18, 0)));
        staffServiceEligibilityRepository.saveAndFlush(new StaffServiceEligibility(assignment, branchService));

        // Patients
        patientUser = userAccountRepository.saveAndFlush(new UserAccount("patient1." + UUID.randomUUID() + "@clinic.test", "hash123"));
        patient = patientProfileRepository.saveAndFlush(new PatientProfile(patientUser, "Can Yilmaz", "+905553332211"));

        otherPatientUser = userAccountRepository.saveAndFlush(new UserAccount("patient2." + UUID.randomUUID() + "@clinic.test", "hash123"));
        otherPatient = patientProfileRepository.saveAndFlush(new PatientProfile(otherPatientUser, "Ayse Demir", "+905554443322"));

        authenticateAs(patient);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        appointmentStatusHistoryJpaRepository.deleteAll();
        appointmentJpaRepository.deleteAll();
    }

    private void authenticateAs(PatientProfile p) {
        UserAccount u = p.getId().equals(patient.getId()) ? patientUser : otherPatientUser;
        ClinicPrincipal principal = new ClinicPrincipal(
                u.getId(),
                u.getEmail(),
                u.getPasswordHash(),
                u.isEnabled(),
                List.of(new SimpleGrantedAuthority(Role.ROLE_PATIENT.name()))
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("TC-013 & TC-016 & TC-017: Successfully books an appointment with PENDING status, valid snapshot and creation history")
    void shouldBookAppointmentSuccessfully() {
        BookingCommand cmd = new BookingCommand(
                patient.getId(),
                doctor.getId(),
                branch.getId(),
                branchService.getId(),
                targetDate,
                LocalTime.of(10, 0)
        );

        Appointment appt = bookingService.bookAppointment(cmd);

        assertThat(appt).isNotNull();
        assertThat(appt.getId()).isNotNull();
        assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(appt.getPatientId()).isEqualTo(patient.getId());
        assertThat(appt.getStaffId()).isEqualTo(doctor.getId());
        assertThat(appt.getBranchId()).isEqualTo(branch.getId());
        assertThat(appt.getBranchServiceId()).isEqualTo(branchService.getId());

        // Snapshot verification (TC-016)
        ServiceSnapshot snapshot = appt.getServiceSnapshot();
        assertThat(snapshot.serviceName()).isEqualTo("Check-Up");
        assertThat(snapshot.durationMinutes()).isEqualTo(30);
        assertThat(snapshot.fee().amount()).isEqualByComparingTo("600.00");
        assertThat(snapshot.fee().currency().getCurrencyCode()).isEqualTo("TRY");

        // Database persistence check
        AppointmentJpaEntity entity = appointmentJpaRepository.findById(appt.getId()).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(entity.getVersion()).isZero();

        // Creation audit event check (TC-017)
        List<AppointmentStatusHistoryJpaEntity> history =
                appointmentStatusHistoryJpaRepository.findByAppointmentIdOrderByOccurredAtAsc(appt.getId());
        assertThat(history).hasSize(1);
        AppointmentStatusHistoryJpaEntity event = history.get(0);
        assertThat(event.getFromStatus()).isNull();
        assertThat(event.getToStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(event.getActorUserId()).isEqualTo(patientUser.getId());
    }

    @Test
    @DisplayName("TC-014: Sequential overlapping appointment is rejected with AppointmentConflictException")
    void shouldRejectOverlappingAppointment() {
        BookingCommand cmd1 = new BookingCommand(patient.getId(), doctor.getId(), branch.getId(), branchService.getId(), targetDate, LocalTime.of(10, 0));
        bookingService.bookAppointment(cmd1);

        authenticateAs(otherPatient);
        // Overlapping request: 10:15 - 10:45
        BookingCommand cmd2 = new BookingCommand(otherPatient.getId(), doctor.getId(), branch.getId(), branchService.getId(), targetDate, LocalTime.of(10, 15));

        assertThatThrownBy(() -> bookingService.bookAppointment(cmd2))
                .isInstanceOf(AppointmentConflictException.class)
                .hasMessageContaining("conflicting appointment");
    }

    @Test
    @DisplayName("TC-014: Touching / adjacent slots (09:30-10:00 and 10:00-10:30) are both valid and accepted")
    void shouldAllowTouchingIntervals() {
        BookingCommand cmd1 = new BookingCommand(patient.getId(), doctor.getId(), branch.getId(), branchService.getId(), targetDate, LocalTime.of(9, 30));
        BookingCommand cmd2 = new BookingCommand(otherPatient.getId(), doctor.getId(), branch.getId(), branchService.getId(), targetDate, LocalTime.of(10, 0));

        Appointment appt1 = bookingService.bookAppointment(cmd1);
        authenticateAs(otherPatient);
        Appointment appt2 = bookingService.bookAppointment(cmd2);

        assertThat(appt1).isNotNull();
        assertThat(appt2).isNotNull();
        assertThat(appointmentJpaRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("TC-012 / FR-012: Full selected service duration must fit within branch hours; 17:45 + 30 min exceeds 18:00 close")
    void shouldRejectIntervalOutsideOpeningHours() {
        // Branch closes at 18:00, 30 min duration starting at 17:45 would end at 18:15
        BookingCommand cmd = new BookingCommand(patient.getId(), doctor.getId(), branch.getId(), branchService.getId(), targetDate, LocalTime.of(17, 45));

        assertThatThrownBy(() -> bookingService.bookAppointment(cmd))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining("branch opening hours");
    }

    @Test
    @DisplayName("TC-011 / SCH-03: Rejects appointment booking when staff has full-day leave exception")
    void shouldRejectDuringFullDayLeave() {
        availabilityExceptionRepository.saveAndFlush(
                new AvailabilityException(assignment, targetDate, AvailabilityExceptionKind.FULL_DAY_LEAVE, null, null, "Medical Conference")
        );

        BookingCommand cmd = new BookingCommand(patient.getId(), doctor.getId(), branch.getId(), branchService.getId(), targetDate, LocalTime.of(11, 0));

        assertThatThrownBy(() -> bookingService.bookAppointment(cmd))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining("full-day leave");
    }

    @Test
    @DisplayName("TC-011 / SCH-03: Rejects appointment booking overlapping with partial leave exception")
    void shouldRejectDuringPartialLeave() {
        // Partial leave 12:00 - 14:00
        availabilityExceptionRepository.saveAndFlush(
                new AvailabilityException(assignment, targetDate, AvailabilityExceptionKind.PARTIAL_LEAVE, LocalTime.of(12, 0), LocalTime.of(14, 0), "Doctor Seminar")
        );

        BookingCommand cmd = new BookingCommand(patient.getId(), doctor.getId(), branch.getId(), branchService.getId(), targetDate, LocalTime.of(12, 30));

        assertThatThrownBy(() -> bookingService.bookAppointment(cmd))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining("not available during requested interval");
    }

    @Test
    @DisplayName("TC-011: Allows appointment booking during overtime exception window")
    void shouldAllowDuringOvertime() {
        // Add overtime on Sunday (weekday 7) when regular weekly availability is empty
        LocalDate sunday = targetDate.minusDays(1); // Sunday
        branchOpeningHoursRepository.saveAndFlush(new BranchOpeningHours(branch, (short) 7, LocalTime.of(9, 0), LocalTime.of(18, 0)));
        availabilityExceptionRepository.saveAndFlush(
                new AvailabilityException(assignment, sunday, AvailabilityExceptionKind.OVERTIME, LocalTime.of(10, 0), LocalTime.of(14, 0), "Emergency Shift")
        );

        BookingCommand cmd = new BookingCommand(patient.getId(), doctor.getId(), branch.getId(), branchService.getId(), sunday, LocalTime.of(10, 30));

        Appointment appt = bookingService.bookAppointment(cmd);
        assertThat(appt).isNotNull();
        assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    @DisplayName("TC-032 / FR-032: Preserves immutable snapshot when catalog price and duration subsequently change")
    void shouldPreserveSnapshotWhenCatalogChanges() {
        BookingCommand cmd = new BookingCommand(patient.getId(), doctor.getId(), branch.getId(), branchService.getId(), targetDate, LocalTime.of(10, 0));
        Appointment appt = bookingService.bookAppointment(cmd);

        // Later: Catalog admin updates the price to 1200 TL and duration to 45 min
        branchService.changePrice(Money.of(new BigDecimal("1200.00"), Currency.getInstance("TRY")));
        branchService.changeDuration(45);
        branchServiceRepository.saveAndFlush(branchService);

        // Reload existing appointment from database
        AppointmentJpaEntity reloaded = appointmentJpaRepository.findById(appt.getId()).orElseThrow();
        Appointment reconstituted = reloaded.toDomain();

        // Must still retain original snapshot values
        assertThat(reconstituted.getServiceSnapshot().fee().amount()).isEqualByComparingTo("600.00");
        assertThat(reconstituted.getServiceSnapshot().durationMinutes()).isEqualTo(30);
        assertThat(reconstituted.getInterval().duration().toMinutes()).isEqualTo(30);
    }

    @Test
    @DisplayName("TC-022: Impersonation attempt is rejected when caller attempts to book for another patient")
    void shouldRejectImpersonationAttempt() {
        // Authenticated as 'patient', but payload supplies 'otherPatient.id'
        BookingCommand cmd = new BookingCommand(otherPatient.getId(), doctor.getId(), branch.getId(), branchService.getId(), targetDate, LocalTime.of(10, 0));

        assertThatThrownBy(() -> bookingService.bookAppointment(cmd))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("cannot book appointment on behalf of another patient");
    }

    @Test
    @DisplayName("Cross-tenant integrity: Rejects booking if branch service belongs to a different branch")
    void shouldRejectCrossTenantMismatch() {
        Branch otherBranch = branchRepository.saveAndFlush(new Branch(org, "Anatolian Clinic", "Europe/Istanbul", "Kadikoy 5"));
        BranchService otherService = branchServiceRepository.saveAndFlush(
                new BranchService(org, otherBranch, serviceDef, 30, Money.of(new BigDecimal("600.00"), Currency.getInstance("TRY")))
        );

        BookingCommand cmd = new BookingCommand(patient.getId(), doctor.getId(), branch.getId(), otherService.getId(), targetDate, LocalTime.of(10, 0));

        assertThatThrownBy(() -> bookingService.bookAppointment(cmd))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining("does not belong to branch");
    }

    @Test
    @DisplayName("TC-033 / FR-033: Rejects nonexistent local time during DST spring-forward gap")
    void shouldRejectDstSpringForwardGap() {
        Department dstDept = departmentRepository.saveAndFlush(new Department(dstBranch, "Dst Dept"));
        BranchService dstService = branchServiceRepository.saveAndFlush(
                new BranchService(org, dstBranch, serviceDef, 30, Money.of(new BigDecimal("100.00"), Currency.getInstance("EUR")))
        );
        StaffAssignment dstAssign = staffAssignmentRepository.saveAndFlush(new StaffAssignment(doctor, dstBranch, dstDept));
        weeklyAvailabilityRepository.saveAndFlush(new WeeklyAvailability(dstAssign, (short) 7, LocalTime.of(2, 0), LocalTime.of(18, 0)));
        staffServiceEligibilityRepository.saveAndFlush(new StaffServiceEligibility(dstAssign, dstService));

        // In Europe/Berlin, on 2027-03-28 at 02:30: clock jumps from 02:00 to 03:00 (nonexistent 02:30)
        LocalDate springForwardDate = LocalDate.of(2027, 3, 28);
        LocalTime gapTime = LocalTime.of(2, 30);

        BookingCommand cmd = new BookingCommand(patient.getId(), doctor.getId(), dstBranch.getId(), dstService.getId(), springForwardDate, gapTime);

        assertThatThrownBy(() -> bookingService.bookAppointment(cmd))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining("Daylight Saving Time gap");
    }

    @Test
    @DisplayName("TC-033 / FR-033: Rejects ambiguous local time during DST fall-back without explicit valid offset")
    void shouldRejectAmbiguousDstOverlapWithoutOffset() {
        Department dstDept = departmentRepository.saveAndFlush(new Department(dstBranch, "Dst Dept 2"));
        BranchService dstService = branchServiceRepository.saveAndFlush(
                new BranchService(org, dstBranch, serviceDef, 30, Money.of(new BigDecimal("100.00"), Currency.getInstance("EUR")))
        );
        StaffAssignment dstAssign = staffAssignmentRepository.saveAndFlush(new StaffAssignment(doctor, dstBranch, dstDept));
        weeklyAvailabilityRepository.saveAndFlush(new WeeklyAvailability(dstAssign, (short) 7, LocalTime.of(2, 0), LocalTime.of(18, 0)));
        staffServiceEligibilityRepository.saveAndFlush(new StaffServiceEligibility(dstAssign, dstService));

        // In Europe/Berlin, on 2027-10-31 at 02:30: clock falls back, 02:30 occurs twice (+02:00 and +01:00)
        LocalDate fallBackDate = LocalDate.of(2027, 10, 31);
        LocalTime ambiguousTime = LocalTime.of(2, 30);

        BookingCommand cmd = new BookingCommand(patient.getId(), doctor.getId(), dstBranch.getId(), dstService.getId(), fallBackDate, ambiguousTime);

        assertThatThrownBy(() -> bookingService.bookAppointment(cmd))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining("ambiguous due to Daylight Saving Time overlap");
    }

    @Test
    @DisplayName("TC-033 / FR-033: Accepts ambiguous local time during DST fall-back when explicit valid offset is provided")
    void shouldAcceptDstOverlapWithExplicitValidOffset() {
        Department dstDept = departmentRepository.saveAndFlush(new Department(dstBranch, "Dst Dept 3"));
        BranchService dstService = branchServiceRepository.saveAndFlush(
                new BranchService(org, dstBranch, serviceDef, 30, Money.of(new BigDecimal("100.00"), Currency.getInstance("EUR")))
        );
        StaffAssignment dstAssign = staffAssignmentRepository.saveAndFlush(new StaffAssignment(doctor, dstBranch, dstDept));
        weeklyAvailabilityRepository.saveAndFlush(new WeeklyAvailability(dstAssign, (short) 7, LocalTime.of(2, 0), LocalTime.of(18, 0)));
        staffServiceEligibilityRepository.saveAndFlush(new StaffServiceEligibility(dstAssign, dstService));

        LocalDate fallBackDate = LocalDate.of(2027, 10, 31);
        LocalTime ambiguousTime = LocalTime.of(2, 30);
        ZoneOffset validOffset = ZoneOffset.ofHours(2); // Europe/Berlin CEST (+02:00)

        BookingCommand cmd = new BookingCommand(patient.getId(), doctor.getId(), dstBranch.getId(), dstService.getId(), fallBackDate, ambiguousTime, validOffset);

        Appointment appt = bookingService.bookAppointment(cmd);
        assertThat(appt).isNotNull();
        assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.PENDING);
    }
}
