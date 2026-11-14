package com.example.clinic;

import com.example.clinic.booking.application.BookingCommand;
import com.example.clinic.booking.application.BookingService;
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
import com.example.clinic.performance.PerformanceMetrics;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repeatable bounded local p95 latency benchmark against real PostgreSQL 17 Testcontainers (NFR-008, TC-044).
 * Seeds 50 doctors and 1,000 existing appointments, runs JVM/DB warmup, measures bounded concurrent
 * slot-search and transactional booking workloads via {@code System.nanoTime()}, and asserts p95 &lt; 1000 ms.
 */
class PerformanceBenchmarkIT extends AbstractIntegrationTest {

    private static final int STAFF_COUNT = 50;
    private static final int BACKGROUND_APPOINTMENTS = 1000;
    private static final int PATIENT_POOL_SIZE = 20;
    private static final int BENCHMARK_THREADS = 4;
    private static final int SLOT_SEARCH_REQUESTS = 40;
    private static final int BOOKING_REQUESTS = 24;
    private static final double P95_THRESHOLD_MS = 1000.0;
    private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");

    @Autowired private AvailabilityCalculator availabilityCalculator;
    @Autowired private BookingService bookingService;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private BranchOpeningHoursRepository branchOpeningHoursRepository;
    @Autowired private ServiceDefinitionRepository serviceDefinitionRepository;
    @Autowired private BranchServiceRepository branchServiceRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private PatientProfileRepository patientProfileRepository;
    @Autowired private StaffProfileRepository staffProfileRepository;
    @Autowired private StaffAssignmentRepository staffAssignmentRepository;
    @Autowired private StaffServiceEligibilityRepository staffServiceEligibilityRepository;
    @Autowired private WeeklyAvailabilityRepository weeklyAvailabilityRepository;
    @Autowired private AppointmentJpaRepository appointmentJpaRepository;
    @Autowired private AppointmentStatusHistoryJpaRepository appointmentStatusHistoryJpaRepository;

    private Organization org;
    private Branch branch;
    private Department department;
    private ServiceDefinition serviceDef;
    private BranchService branchService;
    private final List<UUID> openingHourIds = new ArrayList<>();
    private final List<UUID> userIds = new ArrayList<>();
    private final List<UUID> membershipIds = new ArrayList<>();
    private final List<UUID> staffIds = new ArrayList<>();
    private final List<UUID> patientIds = new ArrayList<>();
    private final List<UUID> assignmentIds = new ArrayList<>();
    private final List<UUID> eligibilityIds = new ArrayList<>();
    private final List<UUID> weeklyAvailabilityIds = new ArrayList<>();
    private final List<UserAccount> patientUserAccounts = new ArrayList<>();
    private LocalDate targetWeekday;

    @BeforeEach
    void setUpRealisticDataset() {
        targetWeekday = LocalDate.now(ISTANBUL).plusWeeks(4);
        while (targetWeekday.getDayOfWeek() != DayOfWeek.TUESDAY) {
            targetWeekday = targetWeekday.plusDays(1);
        }

        Instant now = Instant.now();
        org = organizationRepository.saveAndFlush(new Organization("Perf Benchmark Group " + UUID.randomUUID()));
        branch = branchRepository.saveAndFlush(new Branch(org, "Perf Merkez", "Europe/Istanbul", "İstanbul"));
        department = departmentRepository.saveAndFlush(new Department(branch, "Genel Poliklinik"));

        List<BranchOpeningHours> hours = new ArrayList<>();
        for (short day = 1; day <= 5; day++) {
            BranchOpeningHours oh = new BranchOpeningHours(branch, day, LocalTime.of(8, 0), LocalTime.of(18, 0));
            hours.add(oh);
            openingHourIds.add(oh.getId());
        }
        branchOpeningHoursRepository.saveAllAndFlush(hours);

        serviceDef = serviceDefinitionRepository.saveAndFlush(
                new ServiceDefinition(org, "Perf Muayene " + UUID.randomUUID(), "Performans testi hizmeti")
        );
        branchService = branchServiceRepository.saveAndFlush(
                new BranchService(org, branch, serviceDef, 20, Money.of(new BigDecimal("1000.00"), Currency.getInstance("TRY")))
        );

        String hash = "$2a$12$testHashPlaceholder0000000000000000000000000000000";
        List<UserAccount> usersToSave = new ArrayList<>(STAFF_COUNT + PATIENT_POOL_SIZE);
        List<Membership> membershipsToSave = new ArrayList<>(STAFF_COUNT + PATIENT_POOL_SIZE);
        List<StaffProfile> staffToSave = new ArrayList<>(STAFF_COUNT);
        List<StaffAssignment> assignmentsToSave = new ArrayList<>(STAFF_COUNT);
        List<StaffServiceEligibility> eligibilitiesToSave = new ArrayList<>(STAFF_COUNT);
        List<WeeklyAvailability> availabilitiesToSave = new ArrayList<>(STAFF_COUNT);
        List<PatientProfile> patientsToSave = new ArrayList<>(PATIENT_POOL_SIZE);

        for (int i = 0; i < STAFF_COUNT; i++) {
            UserAccount staffUser = new UserAccount("perf.doc." + i + "." + UUID.randomUUID() + "@clinic.test", hash);
            usersToSave.add(staffUser);
            userIds.add(staffUser.getId());

            Membership membership = new Membership(staffUser, org, branch, Role.ROLE_STAFF);
            membershipsToSave.add(membership);
            membershipIds.add(membership.getId());

            StaffProfile staff = new StaffProfile(staffUser, "Dr. Perf " + i, "+90555100" + String.format("%04d", i), StaffType.DOCTOR, "Dahiliye");
            staffToSave.add(staff);
            staffIds.add(staff.getId());

            StaffAssignment assignment = new StaffAssignment(staff, branch, department);
            assignmentsToSave.add(assignment);
            assignmentIds.add(assignment.getId());

            StaffServiceEligibility eligibility = new StaffServiceEligibility(assignment, branchService);
            eligibilitiesToSave.add(eligibility);
            eligibilityIds.add(eligibility.getId());

            WeeklyAvailability avail = new WeeklyAvailability(
                    assignment, (short) targetWeekday.getDayOfWeek().getValue(), LocalTime.of(9, 0), LocalTime.of(17, 0)
            );
            availabilitiesToSave.add(avail);
            weeklyAvailabilityIds.add(avail.getId());
        }

        for (int i = 0; i < PATIENT_POOL_SIZE; i++) {
            UserAccount patientUser = new UserAccount("perf.pat." + i + "." + UUID.randomUUID() + "@clinic.test", hash);
            usersToSave.add(patientUser);
            userIds.add(patientUser.getId());
            patientUserAccounts.add(patientUser);

            Membership membership = new Membership(patientUser, org, null, Role.ROLE_PATIENT);
            membershipsToSave.add(membership);
            membershipIds.add(membership.getId());

            PatientProfile patient = new PatientProfile(patientUser, "Perf Hasta " + i, "+90555200" + String.format("%04d", i));
            patientsToSave.add(patient);
            patientIds.add(patient.getId());
        }

        userAccountRepository.saveAllAndFlush(usersToSave);
        membershipRepository.saveAllAndFlush(membershipsToSave);
        staffProfileRepository.saveAllAndFlush(staffToSave);
        patientProfileRepository.saveAllAndFlush(patientsToSave);
        staffAssignmentRepository.saveAllAndFlush(assignmentsToSave);
        staffServiceEligibilityRepository.saveAllAndFlush(eligibilitiesToSave);
        weeklyAvailabilityRepository.saveAllAndFlush(availabilitiesToSave);

        // Seed 1,000 existing appointments spread across 50 doctors x 20 slots (distinct days/times in the past/nearby)
        List<AppointmentJpaEntity> appointments = new ArrayList<>(BACKGROUND_APPOINTMENTS);
        List<AppointmentStatusHistoryJpaEntity> histories = new ArrayList<>(BACKGROUND_APPOINTMENTS);
        for (int idx = 0; idx < BACKGROUND_APPOINTMENTS; idx++) {
            int staffIdx = idx % STAFF_COUNT;
            int perStaffSlot = idx / STAFF_COUNT; // 0..19 -> 20 distinct slots per staff
            int dayOffset = perStaffSlot / 5; // 0..3 -> spreads cleanly
            int slotIndex = perStaffSlot % 5; // 09:00..10:20, leaving afternoon (11:00..17:00) open on targetWeekday

            LocalDate apptDate = targetWeekday.plusWeeks(dayOffset + 1);
            LocalTime startLocal = LocalTime.of(9, 0).plusMinutes(slotIndex * 20L);
            Instant startUtc = ZonedDateTime.of(apptDate, startLocal, ISTANBUL).toInstant();
            Instant endUtc = startUtc.plus(Duration.ofMinutes(20));

            UUID apptId = UUID.randomUUID();
            UUID patientId = patientIds.get(idx % PATIENT_POOL_SIZE);
            UUID staffId = staffIds.get(staffIdx);
            UUID actorId = patientUserAccounts.get(idx % PATIENT_POOL_SIZE).getId();

            appointments.add(new AppointmentJpaEntity(
                    apptId, patientId, staffId, branch.getId(), branchService.getId(),
                    startUtc, endUtc, AppointmentStatus.CONFIRMED, serviceDef.getName(), 20,
                    new BigDecimal("1000.00"), "TRY", 0L, now
            ));
            histories.add(new AppointmentStatusHistoryJpaEntity(
                    UUID.randomUUID(), apptId, null, AppointmentStatus.CONFIRMED, actorId, "Seed load", now
            ));
        }

        appointmentJpaRepository.saveAllAndFlush(appointments);
        appointmentStatusHistoryJpaRepository.saveAllAndFlush(histories);
    }

    @AfterEach
    void cleanUpBenchmarkDataset() {
        SecurityContextHolder.clearContext();

        if (branch != null) {
            List<UUID> apptIds = appointmentJpaRepository.findIdsByBranchIdIn(List.of(branch.getId()));
            if (!apptIds.isEmpty()) {
                List<UUID> historyIds = appointmentStatusHistoryJpaRepository.findIdsByAppointmentIdIn(apptIds);
                if (!historyIds.isEmpty()) {
                    appointmentStatusHistoryJpaRepository.deleteAllByIdInBatch(historyIds);
                }
                appointmentJpaRepository.deleteAllByIdInBatch(apptIds);
            }
        }
        if (!weeklyAvailabilityIds.isEmpty()) {
            weeklyAvailabilityRepository.deleteAllByIdInBatch(weeklyAvailabilityIds);
        }
        if (!eligibilityIds.isEmpty()) {
            staffServiceEligibilityRepository.deleteAllByIdInBatch(eligibilityIds);
        }
        if (!assignmentIds.isEmpty()) {
            staffAssignmentRepository.deleteAllByIdInBatch(assignmentIds);
        }
        if (branchService != null) {
            branchServiceRepository.deleteAllByIdInBatch(List.of(branchService.getId()));
        }
        if (serviceDef != null) {
            serviceDefinitionRepository.deleteAllByIdInBatch(List.of(serviceDef.getId()));
        }
        if (department != null) {
            departmentRepository.deleteAllByIdInBatch(List.of(department.getId()));
        }
        if (!openingHourIds.isEmpty()) {
            branchOpeningHoursRepository.deleteAllByIdInBatch(openingHourIds);
        }
        if (!membershipIds.isEmpty()) {
            membershipRepository.deleteAllByIdInBatch(membershipIds);
        }
        if (!patientIds.isEmpty()) {
            patientProfileRepository.deleteAllByIdInBatch(patientIds);
        }
        if (!staffIds.isEmpty()) {
            staffProfileRepository.deleteAllByIdInBatch(staffIds);
        }
        if (!userIds.isEmpty()) {
            userAccountRepository.deleteAllByIdInBatch(userIds);
        }
        if (branch != null) {
            branchRepository.deleteAllByIdInBatch(List.of(branch.getId()));
        }
        if (org != null) {
            organizationRepository.deleteAllByIdInBatch(List.of(org.getId()));
        }
    }

    @Test
    @DisplayName("TC-044 / NFR-008: Local slot search and concurrent booking p95 latency stay below 1000 ms on 50-staff 1000-appointment DB")
    void localP95LatencyMeetsTargetUnderBoundedLoad() throws Exception {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThat(appointmentJpaRepository.findByBranchId(branch.getId())).hasSize(BACKGROUND_APPOINTMENTS);

        // Warmup JVM, Hibernate statement plans, and Hikari connections
        for (int i = 0; i < 5; i++) {
            availabilityCalculator.calculateAvailableSlots(staffIds.get(i), branchService.getId(), targetWeekday);
        }
        authenticateAsPatient(patientUserAccounts.get(0));
        bookingService.bookAppointment(new BookingCommand(
                patientIds.get(0), staffIds.get(0), branch.getId(), branchService.getId(),
                targetWeekday, LocalTime.of(16, 0)
        ));
        SecurityContextHolder.clearContext();

        PerformanceMetrics.Summary slotSearchSummary = runSlotSearchBenchmark();
        PerformanceMetrics.Summary bookingSummary = runBookingBenchmark();

        System.out.println(slotSearchSummary.formatLogLine());
        System.out.println(bookingSummary.formatLogLine());

        assertThat(slotSearchSummary.failureCount()).isZero();
        assertThat(slotSearchSummary.successCount()).isEqualTo(SLOT_SEARCH_REQUESTS);
        assertThat(slotSearchSummary.p95Ms()).isLessThan(P95_THRESHOLD_MS);

        assertThat(bookingSummary.failureCount()).isZero();
        assertThat(bookingSummary.successCount()).isEqualTo(BOOKING_REQUESTS);
        assertThat(bookingSummary.p95Ms()).isLessThan(P95_THRESHOLD_MS);
    }

    private PerformanceMetrics.Summary runSlotSearchBenchmark() throws InterruptedException, ExecutionException {
        ExecutorService pool = Executors.newFixedThreadPool(BENCHMARK_THREADS);
        List<Long> samples = new CopyOnWriteArrayList<>();
        AtomicInteger failures = new AtomicInteger(0);

        try {
            List<Callable<Void>> tasks = new ArrayList<>(SLOT_SEARCH_REQUESTS);
            for (int i = 0; i < SLOT_SEARCH_REQUESTS; i++) {
                final int idx = i;
                tasks.add(() -> {
                    long startNs = System.nanoTime();
                    try {
                        List<TimeSlot> slots = availabilityCalculator.calculateAvailableSlots(
                                staffIds.get(idx % STAFF_COUNT),
                                branchService.getId(),
                                targetWeekday
                        );
                        if (slots.isEmpty()) {
                            failures.incrementAndGet();
                        } else {
                            samples.add(System.nanoTime() - startNs);
                        }
                    } catch (Throwable t) {
                        failures.incrementAndGet();
                    }
                    return null;
                });
            }

            List<Future<Void>> futures = pool.invokeAll(tasks, 60, TimeUnit.SECONDS);
            for (Future<Void> f : futures) {
                f.get();
            }
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }

        return PerformanceMetrics.summarize("availability-slot-search", samples, failures.get());
    }

    private PerformanceMetrics.Summary runBookingBenchmark() throws InterruptedException, ExecutionException {
        ExecutorService pool = Executors.newFixedThreadPool(BENCHMARK_THREADS);
        List<Long> samples = new CopyOnWriteArrayList<>();
        AtomicInteger failures = new AtomicInteger(0);

        try {
            List<Callable<Void>> tasks = new ArrayList<>(BOOKING_REQUESTS);
            for (int i = 0; i < BOOKING_REQUESTS; i++) {
                final int idx = i;
                // Distinct doctor + distinct patient + distinct non-overlapping time slot per request to isolate DB tx latency
                final int patientIdx = (idx % (PATIENT_POOL_SIZE - 1)) + 1;
                final int staffIdx = (idx % (STAFF_COUNT - 1)) + 1;
                final LocalTime slot = LocalTime.of(12, 0).plusMinutes((idx / 8) * 20L);

                tasks.add(() -> {
                    long startNs = System.nanoTime();
                    try {
                        authenticateAsPatient(patientUserAccounts.get(patientIdx));
                        bookingService.bookAppointment(new BookingCommand(
                                patientIds.get(patientIdx),
                                staffIds.get(staffIdx),
                                branch.getId(),
                                branchService.getId(),
                                targetWeekday,
                                slot
                        ));
                        samples.add(System.nanoTime() - startNs);
                    } catch (Throwable t) {
                        failures.incrementAndGet();
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                    return null;
                });
            }

            List<Future<Void>> futures = pool.invokeAll(tasks, 60, TimeUnit.SECONDS);
            for (Future<Void> f : futures) {
                f.get();
            }
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }

        return PerformanceMetrics.summarize("booking-tx", samples, failures.get());
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
}
