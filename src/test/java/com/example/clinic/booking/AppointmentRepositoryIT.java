package com.example.clinic.booking;

import com.example.clinic.AbstractIntegrationTest;
import com.example.clinic.booking.domain.Appointment;
import com.example.clinic.booking.domain.AppointmentStatus;
import com.example.clinic.booking.domain.ServiceSnapshot;
import com.example.clinic.booking.infrastructure.adapter.DatabaseAppointmentSchedulePort;
import com.example.clinic.booking.infrastructure.persistence.AppointmentJpaEntity;
import com.example.clinic.booking.infrastructure.persistence.AppointmentJpaRepository;
import com.example.clinic.catalog.domain.BranchService;
import com.example.clinic.catalog.domain.ServiceDefinition;
import com.example.clinic.catalog.infrastructure.BranchServiceRepository;
import com.example.clinic.catalog.infrastructure.ServiceDefinitionRepository;
import com.example.clinic.common.domain.Money;
import com.example.clinic.common.domain.TimeInterval;
import com.example.clinic.identity.domain.PatientProfile;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository and adapter integration tests validating PostgreSQL roundtrip,
 * version incrementation, and {@link DatabaseAppointmentSchedulePort} bridge to {@link AvailabilityCalculator}.
 */
class AppointmentRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private AppointmentJpaRepository appointmentJpaRepository;

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
    private ServiceDefinitionRepository serviceDefinitionRepository;

    @Autowired
    private BranchServiceRepository branchServiceRepository;

    private Organization org;
    private Branch branch;
    private Department dept;
    private ServiceDefinition serviceDef;
    private BranchService branchService;
    private StaffProfile staff;
    private StaffAssignment assignment;
    private PatientProfile patient;
    private LocalDate targetDate;

    @BeforeEach
    void setUp() {
        targetDate = LocalDate.now().plusWeeks(2);
        while (targetDate.getDayOfWeek().getValue() != 1) { // Monday
            targetDate = targetDate.plusDays(1);
        }

        org = organizationRepository.saveAndFlush(new Organization("Alpha Health"));
        branch = branchRepository.saveAndFlush(new Branch(org, "Main Branch", "Europe/Istanbul", "Ataturk Cad 12"));
        dept = departmentRepository.saveAndFlush(new Department(branch, "General Polyclinic"));

        branchOpeningHoursRepository.saveAndFlush(new BranchOpeningHours(branch, (short) 1, LocalTime.of(9, 0), LocalTime.of(17, 0)));

        serviceDef = serviceDefinitionRepository.saveAndFlush(new ServiceDefinition(org, "General Exam", "General Examination"));
        branchService = branchServiceRepository.saveAndFlush(
                new BranchService(org, branch, serviceDef, 30, Money.of(new BigDecimal("500.00"), Currency.getInstance("TRY")))
        );

        UserAccount staffUser = userAccountRepository.saveAndFlush(new UserAccount("staff." + UUID.randomUUID() + "@clinic.test", "hash123"));
        staff = staffProfileRepository.saveAndFlush(new StaffProfile(staffUser, "Dr. Ali Veli", "+905551234567", StaffType.DOCTOR, "General Medicine"));

        assignment = staffAssignmentRepository.saveAndFlush(new StaffAssignment(staff, branch, dept));
        weeklyAvailabilityRepository.saveAndFlush(new WeeklyAvailability(assignment, (short) 1, LocalTime.of(9, 0), LocalTime.of(17, 0)));
        staffServiceEligibilityRepository.saveAndFlush(new StaffServiceEligibility(assignment, branchService));

        UserAccount patientUser = userAccountRepository.saveAndFlush(new UserAccount("patient." + UUID.randomUUID() + "@clinic.test", "hash123"));
        patient = patientProfileRepository.saveAndFlush(new PatientProfile(patientUser, "Zeynep Kaya", "+905559876543"));
    }

    @AfterEach
    void tearDown() {
        appointmentJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("TC-016: Preserves all aggregate fields, version, interval, and snapshot in reversible PostgreSQL roundtrip")
    void shouldRestoreAggregateAfterJpaRoundTrip() {
        ZoneId branchZone = ZoneId.of(branch.getTimeZone());
        Instant start = targetDate.atTime(10, 0).atZone(branchZone).toInstant();
        Instant end = start.plus(30, ChronoUnit.MINUTES);
        TimeInterval interval = TimeInterval.of(start, end);

        ServiceSnapshot snapshot = new ServiceSnapshot(
                branchService.getId(),
                "General Exam",
                30,
                Money.of(new BigDecimal("500.00"), Currency.getInstance("TRY"))
        );

        Appointment pureDomain = Appointment.create(
                UUID.randomUUID(),
                patient.getId(),
                staff.getId(),
                branch.getId(),
                branchService.getId(),
                interval,
                snapshot,
                Instant.now()
        );

        AppointmentJpaEntity entity = AppointmentJpaEntity.fromDomain(pureDomain, 0L);
        AppointmentJpaEntity saved = appointmentJpaRepository.saveAndFlush(entity);

        AppointmentJpaEntity reloaded = appointmentJpaRepository.findById(saved.getId()).orElseThrow();
        Appointment restored = reloaded.toDomain();

        assertThat(restored.getId()).isEqualTo(pureDomain.getId());
        assertThat(restored.getPatientId()).isEqualTo(pureDomain.getPatientId());
        assertThat(restored.getStaffId()).isEqualTo(pureDomain.getStaffId());
        assertThat(restored.getBranchId()).isEqualTo(pureDomain.getBranchId());
        assertThat(restored.getBranchServiceId()).isEqualTo(pureDomain.getBranchServiceId());
        assertThat(restored.getInterval().startAt()).isEqualTo(start);
        assertThat(restored.getInterval().endAt()).isEqualTo(end);
        assertThat(restored.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(restored.getVersion()).isEqualTo(0L);

        ServiceSnapshot restoredSnapshot = restored.getServiceSnapshot();
        assertThat(restoredSnapshot.serviceName()).isEqualTo("General Exam");
        assertThat(restoredSnapshot.durationMinutes()).isEqualTo(30);
        assertThat(restoredSnapshot.fee().amount()).isEqualByComparingTo("500.00");
        assertThat(restoredSnapshot.fee().currency().getCurrencyCode()).isEqualTo("TRY");
    }

    @Test
    @DisplayName("Optimistic locking: @Version increments when entity is updated")
    void shouldUpdateVersionOnEntityUpdate() {
        ZoneId branchZone = ZoneId.of(branch.getTimeZone());
        Instant start = targetDate.atTime(11, 0).atZone(branchZone).toInstant();
        Instant end = start.plus(30, ChronoUnit.MINUTES);

        AppointmentJpaEntity entity = new AppointmentJpaEntity(
                UUID.randomUUID(),
                patient.getId(),
                staff.getId(),
                branch.getId(),
                branchService.getId(),
                start,
                end,
                AppointmentStatus.PENDING,
                "General Exam",
                30,
                new BigDecimal("500.00"),
                "TRY",
                0L,
                Instant.now()
        );

        AppointmentJpaEntity saved = appointmentJpaRepository.saveAndFlush(entity);
        assertThat(saved.getVersion()).isZero();

        saved.setStatus(AppointmentStatus.CONFIRMED);
        AppointmentJpaEntity updated = appointmentJpaRepository.saveAndFlush(saved);
        assertThat(updated.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("WP-S06-03: DatabaseAppointmentSchedulePort bridges committed appointments to AvailabilityCalculator")
    void shouldReflectBlockedSlotsInAvailabilityCalculator() {
        ZoneId branchZone = ZoneId.of(branch.getTimeZone());

        // Initially: Check available slots for targetDate (09:00 - 17:00, 30 min duration = 16 slots)
        List<TimeSlot> initialSlots = availabilityCalculator.calculateAvailableSlots(staff.getId(), branchService.getId(), targetDate);
        assertThat(initialSlots).isNotEmpty();
        assertThat(initialSlots).contains(new TimeSlot(LocalTime.of(10, 0), LocalTime.of(10, 30)));

        // Create and commit a PENDING appointment at 10:00 - 10:30
        Instant start = targetDate.atTime(10, 0).atZone(branchZone).toInstant();
        Instant end = start.plus(30, ChronoUnit.MINUTES);

        appointmentJpaRepository.saveAndFlush(new AppointmentJpaEntity(
                UUID.randomUUID(),
                patient.getId(),
                staff.getId(),
                branch.getId(),
                branchService.getId(),
                start,
                end,
                AppointmentStatus.PENDING,
                "General Exam",
                30,
                new BigDecimal("500.00"),
                "TRY",
                0L,
                Instant.now()
        ));

        // Also create a CANCELLED appointment at 11:00 - 11:30 (should NOT block slot per ADR-003)
        Instant cancelStart = targetDate.atTime(11, 0).atZone(branchZone).toInstant();
        Instant cancelEnd = cancelStart.plus(30, ChronoUnit.MINUTES);
        appointmentJpaRepository.saveAndFlush(new AppointmentJpaEntity(
                UUID.randomUUID(),
                patient.getId(),
                staff.getId(),
                branch.getId(),
                branchService.getId(),
                cancelStart,
                cancelEnd,
                AppointmentStatus.CANCELLED,
                "General Exam",
                30,
                new BigDecimal("500.00"),
                "TRY",
                0L,
                Instant.now()
        ));

        // Also create a REJECTED appointment at 14:00 - 14:30 (should NOT block slot per ADR-003)
        Instant rejectStart = targetDate.atTime(14, 0).atZone(branchZone).toInstant();
        Instant rejectEnd = rejectStart.plus(30, ChronoUnit.MINUTES);
        appointmentJpaRepository.saveAndFlush(new AppointmentJpaEntity(
                UUID.randomUUID(),
                patient.getId(),
                staff.getId(),
                branch.getId(),
                branchService.getId(),
                rejectStart,
                rejectEnd,
                AppointmentStatus.REJECTED,
                "General Exam",
                30,
                new BigDecimal("500.00"),
                "TRY",
                0L,
                Instant.now()
        ));

        // Re-calculate available slots
        List<TimeSlot> slotsAfterBooking = availabilityCalculator.calculateAvailableSlots(staff.getId(), branchService.getId(), targetDate);

        // 10:00 - 10:30 slot must now disappear from available slots!
        assertThat(slotsAfterBooking).doesNotContain(new TimeSlot(LocalTime.of(10, 0), LocalTime.of(10, 30)));

        // 11:00 - 11:30 (CANCELLED) and 14:00 - 14:30 (REJECTED) must remain available!
        assertThat(slotsAfterBooking).contains(new TimeSlot(LocalTime.of(11, 0), LocalTime.of(11, 30)));
        assertThat(slotsAfterBooking).contains(new TimeSlot(LocalTime.of(14, 0), LocalTime.of(14, 30)));
    }
}
