package com.example.clinic.booking;

import com.example.clinic.AbstractIntegrationTest;
import com.example.clinic.booking.application.AppointmentApprovalService;
import com.example.clinic.booking.application.AppointmentConflictException;
import com.example.clinic.booking.application.BookingCommand;
import com.example.clinic.booking.application.BookingService;
import com.example.clinic.booking.domain.AppointmentDomainException;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deterministic multi-threaded integration test suite validating PostgreSQL row-level locking
 * during concurrent appointment lifecycle transitions (Section 8.2).
 */
class AppointmentLifecycleConcurrencyIT extends AbstractIntegrationTest {

    private static final Currency TRY = Currency.getInstance("TRY");

    @Autowired
    private AppointmentApprovalService approvalService;

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

    private Organization org;
    private Branch branch;
    private Department dept;
    private ServiceDefinition serviceDef;
    private BranchService branchService;

    private UserAccount doctorUser;
    private StaffProfile doctor;
    private UserAccount managerUser;

    private UserAccount patient1User;
    private PatientProfile patient1;
    private UserAccount patient2User;
    private PatientProfile patient2;

    private LocalDate targetDate;
    private Instant appointmentStartUtc;
    private Instant appointmentEndUtc;

    @BeforeEach
    void setUp() {
        targetDate = LocalDate.now().plusWeeks(3);
        while (targetDate.getDayOfWeek().getValue() != 1) {
            targetDate = targetDate.plusDays(1);
        }

        appointmentStartUtc = targetDate.atTime(11, 0).atZone(ZoneId.of("Europe/Istanbul")).toInstant();
        appointmentEndUtc = targetDate.atTime(11, 30).atZone(ZoneId.of("Europe/Istanbul")).toInstant();

        org = organizationRepository.saveAndFlush(new Organization("Concurrency Health Org"));
        branch = branchRepository.saveAndFlush(new Branch(org, "Concurrency Clinic", "Europe/Istanbul", "Main St 10"));
        dept = departmentRepository.saveAndFlush(new Department(branch, "General Medicine"));

        branchOpeningHoursRepository.saveAndFlush(
                new BranchOpeningHours(branch, (short) 1, LocalTime.of(8, 0), LocalTime.of(20, 0))
        );

        serviceDef = serviceDefinitionRepository.saveAndFlush(new ServiceDefinition(org, "Check-Up", "Routine check"));
        branchService = branchServiceRepository.saveAndFlush(
                new BranchService(org, branch, serviceDef, 30, Money.of(new BigDecimal("800.00"), TRY))
        );

        // Doctor
        doctorUser = userAccountRepository.saveAndFlush(new UserAccount("dr.conc." + UUID.randomUUID() + "@clinic.test", "hash123"));
        doctor = staffProfileRepository.saveAndFlush(new StaffProfile(doctorUser, "Dr. Concurrency", "+905553334455", StaffType.DOCTOR, "General Medicine"));
        membershipRepository.saveAndFlush(new Membership(doctorUser, org, branch, Role.ROLE_STAFF));

        StaffAssignment assignment = staffAssignmentRepository.saveAndFlush(new StaffAssignment(doctor, branch, dept));
        weeklyAvailabilityRepository.saveAndFlush(new WeeklyAvailability(assignment, (short) 1, LocalTime.of(9, 0), LocalTime.of(18, 0)));
        staffServiceEligibilityRepository.saveAndFlush(new StaffServiceEligibility(assignment, branchService));

        // Manager
        managerUser = userAccountRepository.saveAndFlush(new UserAccount("mgr.conc." + UUID.randomUUID() + "@clinic.test", "hash123"));
        membershipRepository.saveAndFlush(new Membership(managerUser, org, branch, Role.ROLE_ADMIN));

        // Patients
        patient1User = userAccountRepository.saveAndFlush(new UserAccount("pat1.conc." + UUID.randomUUID() + "@clinic.test", "hash123"));
        patient1 = patientProfileRepository.saveAndFlush(new PatientProfile(patient1User, "Hasta Bir", "+905551111111"));
        membershipRepository.saveAndFlush(new Membership(patient1User, org, null, Role.ROLE_PATIENT));

        patient2User = userAccountRepository.saveAndFlush(new UserAccount("pat2.conc." + UUID.randomUUID() + "@clinic.test", "hash123"));
        patient2 = patientProfileRepository.saveAndFlush(new PatientProfile(patient2User, "Hasta Iki", "+905552222222"));
        membershipRepository.saveAndFlush(new Membership(patient2User, org, null, Role.ROLE_PATIENT));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        appointmentStatusHistoryJpaRepository.deleteAll();
        appointmentJpaRepository.deleteAll();
    }

    private AppointmentJpaEntity createPendingFixture() {
        AppointmentJpaEntity entity = new AppointmentJpaEntity(
                UUID.randomUUID(),
                patient1.getId(),
                doctor.getId(),
                branch.getId(),
                branchService.getId(),
                appointmentStartUtc,
                appointmentEndUtc,
                AppointmentStatus.PENDING,
                "Check-Up",
                30,
                new BigDecimal("800.00"),
                "TRY",
                0L,
                Instant.now()
        );
        AppointmentJpaEntity saved = appointmentJpaRepository.saveAndFlush(entity);

        AppointmentStatusHistoryJpaEntity initial = new AppointmentStatusHistoryJpaEntity(
                UUID.randomUUID(),
                saved.getId(),
                null,
                AppointmentStatus.PENDING,
                patient1User.getId(),
                null,
                Instant.now()
        );
        appointmentStatusHistoryJpaRepository.saveAndFlush(initial);
        return saved;
    }

    private void authenticate(UserAccount user, Role role) {
        ClinicPrincipal principal = new ClinicPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.isEnabled(),
                List.of(new SimpleGrantedAuthority(role.name()))
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("Concurrency: Two concurrent approvals -> exactly 1 succeeds, 1 fails with transition error, no duplicate history")
    void concurrentApprovals_onlyOneSucceedsAndNoDuplicateHistory() throws InterruptedException {
        AppointmentJpaEntity fixture = createPendingFixture();
        UUID appointmentId = fixture.getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch latch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Throwable> exceptions = new CopyOnWriteArrayList<>();

        // Thread 1: Doctor approves
        executor.submit(() -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);
                approvalService.approveAppointment(appointmentId, doctorUser.getId());
                successCount.incrementAndGet();
            } catch (Throwable t) {
                failureCount.incrementAndGet();
                exceptions.add(t);
            } finally {
                latch.countDown();
            }
        });

        // Thread 2: Manager approves
        executor.submit(() -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);
                approvalService.approveAppointment(appointmentId, managerUser.getId());
                successCount.incrementAndGet();
            } catch (Throwable t) {
                failureCount.incrementAndGet();
                exceptions.add(t);
            } finally {
                latch.countDown();
            }
        });

        boolean finished = latch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);
        assertThat(exceptions.get(0)).isInstanceOf(AppointmentDomainException.class);

        // State is CONFIRMED
        AppointmentJpaEntity reloaded = appointmentJpaRepository.findById(appointmentId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);

        // Exactly 2 history entries: NULL -> PENDING and PENDING -> CONFIRMED (NO duplicate approval)
        List<AppointmentStatusHistoryJpaEntity> history = appointmentStatusHistoryJpaRepository
                .findByAppointmentIdOrderByOccurredAtAsc(appointmentId);
        assertThat(history).hasSize(2);
    }

    @Test
    @DisplayName("Concurrency: Approval versus rejection -> exactly 1 winner, consistent outcome")
    void concurrentApprovalAndRejection_oneWinnerConsistentOutcome() throws InterruptedException {
        AppointmentJpaEntity fixture = createPendingFixture();
        UUID appointmentId = fixture.getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch latch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Thread 1: Doctor approves
        executor.submit(() -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);
                approvalService.approveAppointment(appointmentId, doctorUser.getId());
                successCount.incrementAndGet();
            } catch (Throwable t) {
                failureCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        // Thread 2: Manager rejects
        executor.submit(() -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);
                approvalService.rejectAppointment(appointmentId, managerUser.getId(), "Kontenjan doldu");
                successCount.incrementAndGet();
            } catch (Throwable t) {
                failureCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        boolean finished = latch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);

        AppointmentJpaEntity reloaded = appointmentJpaRepository.findById(appointmentId).orElseThrow();
        assertThat(reloaded.getStatus()).isIn(AppointmentStatus.CONFIRMED, AppointmentStatus.REJECTED);

        List<AppointmentStatusHistoryJpaEntity> history = appointmentStatusHistoryJpaRepository
                .findByAppointmentIdOrderByOccurredAtAsc(appointmentId);
        assertThat(history).hasSize(2);
        assertThat(history.get(1).getToStatus()).isEqualTo(reloaded.getStatus());
    }

    @Test
    @DisplayName("Concurrency: Approval versus cancellation -> resolved consistently without corrupted state")
    void concurrentApprovalAndCancellation_consistentOutcome() throws InterruptedException {
        AppointmentJpaEntity fixture = createPendingFixture();
        UUID appointmentId = fixture.getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch latch = new CountDownLatch(2);

        // Thread 1: Doctor approves
        executor.submit(() -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);
                approvalService.approveAppointment(appointmentId, doctorUser.getId());
            } catch (Throwable ignored) {
            } finally {
                latch.countDown();
            }
        });

        // Thread 2: Patient cancels
        executor.submit(() -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);
                approvalService.cancelAppointment(appointmentId, patient1User.getId(), "Acil iptal");
            } catch (Throwable ignored) {
            } finally {
                latch.countDown();
            }
        });

        boolean finished = latch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();

        AppointmentJpaEntity reloaded = appointmentJpaRepository.findById(appointmentId).orElseThrow();
        // Valid final outcomes: either CANCELLED (if cancellation won or cancelled after approval) or CONFIRMED (if approval won and cancel failed)
        assertThat(reloaded.getStatus()).isIn(AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED);

        List<AppointmentStatusHistoryJpaEntity> history = appointmentStatusHistoryJpaRepository
                .findByAppointmentIdOrderByOccurredAtAsc(appointmentId);
        // History must have between 2 and 3 entries (never corrupted)
        assertThat(history.size()).isBetween(2, 3);
        assertThat(history.get(history.size() - 1).getToStatus()).isEqualTo(reloaded.getStatus());
    }

    @Test
    @DisplayName("Concurrency: Cancellation followed by concurrent rebooking -> exactly 1 rebooking succeeds, loser gets 409 conflict")
    void cancellationFollowedByConcurrentRebooking_slotIsRebookable() throws InterruptedException {
        AppointmentJpaEntity fixture = createPendingFixture();
        UUID appointmentId = fixture.getId();

        // 1. Cancel the initial appointment
        approvalService.cancelAppointment(appointmentId, patient1User.getId(), "Yeniden rezerve edilsin");

        AppointmentJpaEntity cancelled = appointmentJpaRepository.findById(appointmentId).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);

        // 2. Two independent patient threads attempt to rebook the now-vacant 11:00-11:30 slot simultaneously
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch latch = new CountDownLatch(2);

        AtomicInteger bookingSuccesses = new AtomicInteger(0);
        AtomicInteger bookingConflicts = new AtomicInteger(0);

        // Worker 1: Patient 1 rebooks
        executor.submit(() -> {
            try {
                authenticate(patient1User, Role.ROLE_PATIENT);
                barrier.await(5, TimeUnit.SECONDS);

                BookingCommand cmd = new BookingCommand(
                        patient1.getId(),
                        doctor.getId(),
                        branch.getId(),
                        branchService.getId(),
                        targetDate,
                        LocalTime.of(11, 0),
                        null
                );
                bookingService.bookAppointment(cmd);
                bookingSuccesses.incrementAndGet();
            } catch (AppointmentConflictException e) {
                bookingConflicts.incrementAndGet();
            } catch (Throwable ignored) {
            } finally {
                latch.countDown();
            }
        });

        // Worker 2: Patient 2 rebooks
        executor.submit(() -> {
            try {
                authenticate(patient2User, Role.ROLE_PATIENT);
                barrier.await(5, TimeUnit.SECONDS);

                BookingCommand cmd = new BookingCommand(
                        patient2.getId(),
                        doctor.getId(),
                        branch.getId(),
                        branchService.getId(),
                        targetDate,
                        LocalTime.of(11, 0),
                        null
                );
                bookingService.bookAppointment(cmd);
                bookingSuccesses.incrementAndGet();
            } catch (AppointmentConflictException e) {
                bookingConflicts.incrementAndGet();
            } catch (Throwable ignored) {
            } finally {
                latch.countDown();
            }
        });

        boolean finished = latch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(bookingSuccesses.get()).isEqualTo(1);
        assertThat(bookingConflicts.get()).isEqualTo(1);

        // Verify there is exactly 1 active appointment in the interval
        List<AppointmentJpaEntity> blockingAppointments = appointmentJpaRepository.findOverlappingAppointmentsForStaff(
                doctor.getId(),
                appointmentStartUtc,
                appointmentEndUtc,
                List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED)
        );
        assertThat(blockingAppointments).hasSize(1);
    }

    @Test
    @DisplayName("Concurrency: Completion versus cancellation -> resolved deterministically without race condition")
    void concurrentCompletionAndCancellation_consistentOutcome() throws InterruptedException {
        AppointmentJpaEntity fixture = createPendingFixture();
        fixture.setStatus(AppointmentStatus.CONFIRMED);
        appointmentJpaRepository.saveAndFlush(fixture);
        UUID appointmentId = fixture.getId();

        // Use custom clock service where time is after appointment end
        java.time.Clock clockAfterEnd = java.time.Clock.fixed(appointmentEndUtc.plus(Duration.ofMinutes(15)), ZoneId.of("Europe/Istanbul"));
        AppointmentApprovalService serviceAfterEnd = new AppointmentApprovalService(
                appointmentJpaRepository,
                appointmentStatusHistoryJpaRepository,
                userAccountRepository,
                staffProfileRepository,
                patientProfileRepository,
                new com.example.clinic.identity.application.AccessPolicy(
                        userAccountRepository,
                        membershipRepository,
                        patientProfileRepository,
                        staffProfileRepository,
                        branchRepository
                ),
                new com.example.clinic.common.security.SecurityContextFacade(userAccountRepository),
                clockAfterEnd
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch latch = new CountDownLatch(2);

        // Thread 1: Doctor completes
        executor.submit(() -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);
                serviceAfterEnd.completeAppointment(appointmentId, doctorUser.getId());
            } catch (Throwable ignored) {
            } finally {
                latch.countDown();
            }
        });

        // Thread 2: Patient attempts to cancel (fails because appointment has already started/ended relative to clockAfterEnd)
        executor.submit(() -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);
                serviceAfterEnd.cancelAppointment(appointmentId, patient1User.getId(), "Geç kalındı");
            } catch (Throwable ignored) {
            } finally {
                latch.countDown();
            }
        });

        boolean finished = latch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();

        AppointmentJpaEntity reloaded = appointmentJpaRepository.findById(appointmentId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);

        List<AppointmentStatusHistoryJpaEntity> history = appointmentStatusHistoryJpaRepository
                .findByAppointmentIdOrderByOccurredAtAsc(appointmentId);
        // Initial PENDING + 1 COMPLETED
        assertThat(history).hasSize(2);
        assertThat(history.get(1).getToStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }
}
