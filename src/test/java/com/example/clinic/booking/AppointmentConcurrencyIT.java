package com.example.clinic.booking;

import com.example.clinic.AbstractIntegrationTest;
import com.example.clinic.booking.application.AppointmentConflictException;
import com.example.clinic.booking.application.BookingCommand;
import com.example.clinic.booking.application.BookingService;
import com.example.clinic.booking.application.PatientOverlapException;
import com.example.clinic.booking.domain.Appointment;
import com.example.clinic.booking.domain.AppointmentStatus;
import com.example.clinic.booking.infrastructure.persistence.AppointmentJpaEntity;
import com.example.clinic.booking.infrastructure.persistence.AppointmentJpaRepository;
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
import com.example.clinic.staff.infrastructure.AvailabilityExceptionRepository;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deterministic multi-threaded concurrency integration tests validating PostgreSQL pessimistic locking.
 * <p>
 * Requirements:
 * <ul>
 *   <li>FR-015 / TC-015: Two simultaneous independent DB transactions must never double-book the same staff.</li>
 *   <li>FR-029 / TC-029: Two simultaneous independent DB transactions must never double-book the same patient.</li>
 *   <li>Staff global overlap across different branches must serialize and fail the loser.</li>
 *   <li>Different staff members booking simultaneously must proceed without global blocking.</li>
 * </ul>
 * Note: Test methods are intentionally NOT @Transactional around both worker threads,
 * guaranteeing true physical database connections and transaction isolation.
 */
class AppointmentConcurrencyIT extends AbstractIntegrationTest {

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

    @Autowired
    private MembershipRepository membershipRepository;

    private Organization org;
    private Branch branchA;
    private Branch branchB;
    private Department deptA;
    private Department deptB;
    private ServiceDefinition serviceDef;
    private BranchService branchServiceA;
    private BranchService branchServiceB;
    private StaffProfile doctor1;
    private StaffProfile doctor2;
    private PatientProfile patient1;
    private PatientProfile patient2;
    private UserAccount patUser1;
    private UserAccount patUser2;
    private LocalDate targetDate;

    @BeforeEach
    void setUpFixtures() {
        // Target date strictly in the future (next Monday)
        targetDate = LocalDate.now().plusWeeks(2);
        while (targetDate.getDayOfWeek().getValue() != 1) { // Monday
            targetDate = targetDate.plusDays(1);
        }

        org = organizationRepository.saveAndFlush(new Organization("Concurrency Health Group"));

        branchA = branchRepository.saveAndFlush(new Branch(org, "Branch Alpha", "Europe/Istanbul", "Main St 1"));
        branchB = branchRepository.saveAndFlush(new Branch(org, "Branch Beta", "Europe/Istanbul", "Second Ave 2"));

        deptA = departmentRepository.saveAndFlush(new Department(branchA, "Cardiology A"));
        deptB = departmentRepository.saveAndFlush(new Department(branchB, "Cardiology B"));

        // Opening hours 08:00 - 18:00 for Monday (weekday 1)
        branchOpeningHoursRepository.saveAndFlush(new BranchOpeningHours(branchA, (short) 1, LocalTime.of(8, 0), LocalTime.of(18, 0)));
        branchOpeningHoursRepository.saveAndFlush(new BranchOpeningHours(branchB, (short) 1, LocalTime.of(8, 0), LocalTime.of(18, 0)));

        serviceDef = serviceDefinitionRepository.saveAndFlush(new ServiceDefinition(org, "Cardio Exam", "Routine Cardiology"));
        branchServiceA = branchServiceRepository.saveAndFlush(
                new BranchService(org, branchA, serviceDef, 30, Money.of(new BigDecimal("750.00"), Currency.getInstance("TRY")))
        );
        branchServiceB = branchServiceRepository.saveAndFlush(
                new BranchService(org, branchB, serviceDef, 30, Money.of(new BigDecimal("800.00"), Currency.getInstance("TRY")))
        );

        // Doctor 1
        UserAccount docUser1 = userAccountRepository.saveAndFlush(new UserAccount("doc1." + UUID.randomUUID() + "@clinic.test", "hash123"));
        doctor1 = staffProfileRepository.saveAndFlush(new StaffProfile(docUser1, "Dr. Alice Smith", "+905550000001", StaffType.DOCTOR, "Cardiology"));

        StaffAssignment assign1A = staffAssignmentRepository.saveAndFlush(new StaffAssignment(doctor1, branchA, deptA));
        weeklyAvailabilityRepository.saveAndFlush(new WeeklyAvailability(assign1A, (short) 1, LocalTime.of(8, 0), LocalTime.of(18, 0)));
        staffServiceEligibilityRepository.saveAndFlush(new StaffServiceEligibility(assign1A, branchServiceA));

        // Doctor 1 also assigned to Branch B
        StaffAssignment assign1B = staffAssignmentRepository.saveAndFlush(new StaffAssignment(doctor1, branchB, deptB));
        weeklyAvailabilityRepository.saveAndFlush(new WeeklyAvailability(assign1B, (short) 1, LocalTime.of(8, 0), LocalTime.of(18, 0)));
        staffServiceEligibilityRepository.saveAndFlush(new StaffServiceEligibility(assign1B, branchServiceB));

        // Doctor 2
        UserAccount docUser2 = userAccountRepository.saveAndFlush(new UserAccount("doc2." + UUID.randomUUID() + "@clinic.test", "hash123"));
        doctor2 = staffProfileRepository.saveAndFlush(new StaffProfile(docUser2, "Dr. Bob Jones", "+905550000002", StaffType.DOCTOR, "Cardiology"));

        StaffAssignment assign2A = staffAssignmentRepository.saveAndFlush(new StaffAssignment(doctor2, branchA, deptA));
        weeklyAvailabilityRepository.saveAndFlush(new WeeklyAvailability(assign2A, (short) 1, LocalTime.of(8, 0), LocalTime.of(18, 0)));
        staffServiceEligibilityRepository.saveAndFlush(new StaffServiceEligibility(assign2A, branchServiceA));

        // Patients
        patUser1 = userAccountRepository.saveAndFlush(new UserAccount("pat1." + UUID.randomUUID() + "@clinic.test", "hash123"));
        patient1 = patientProfileRepository.saveAndFlush(new PatientProfile(patUser1, "John Doe", "+905551111111"));

        patUser2 = userAccountRepository.saveAndFlush(new UserAccount("pat2." + UUID.randomUUID() + "@clinic.test", "hash123"));
        patient2 = patientProfileRepository.saveAndFlush(new PatientProfile(patUser2, "Jane Roe", "+905552222222"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        appointmentStatusHistoryJpaRepository.deleteAll();
        appointmentJpaRepository.deleteAll();
    }

    private void authenticateAs(PatientProfile patient) {
        UserAccount user = patient.getId().equals(patient1.getId()) ? patUser1 : patUser2;
        ClinicPrincipal principal = new ClinicPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.isEnabled(),
                List.of(new SimpleGrantedAuthority(Role.ROLE_PATIENT.name()))
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("TC-015 / FR-015: Two simultaneous independent DB transactions must never double-book the same staff slot")
    void concurrentBooking_onlyOneSucceedsAndOtherFails() throws Exception {
        LocalTime slotTime = LocalTime.of(10, 0);
        BookingCommand cmd1 = new BookingCommand(patient1.getId(), doctor1.getId(), branchA.getId(), branchServiceA.getId(), targetDate, slotTime);
        BookingCommand cmd2 = new BookingCommand(patient2.getId(), doctor1.getId(), branchA.getId(), branchServiceA.getId(), targetDate, slotTime);

        List<Appointment> successes = new CopyOnWriteArrayList<>();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch doneLatch = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            executor.submit(() -> {
                try {
                    authenticateAs(patient1);
                    barrier.await(5, TimeUnit.SECONDS);
                    Appointment appt = bookingService.bookAppointment(cmd1);
                    successes.add(appt);
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    SecurityContextHolder.clearContext();
                    doneLatch.countDown();
                }
            });

            executor.submit(() -> {
                try {
                    authenticateAs(patient2);
                    barrier.await(5, TimeUnit.SECONDS);
                    Appointment appt = bookingService.bookAppointment(cmd2);
                    successes.add(appt);
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    SecurityContextHolder.clearContext();
                    doneLatch.countDown();
                }
            });

            boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
            assertThat(finished).as("Both concurrent threads must finish within timeout").isTrue();
        } finally {
            executor.shutdownNow();
        }

        // Assert exactly one winner and one typed conflict failure
        assertThat(successes).hasSize(1);
        assertThat(failures).hasSize(1);
        assertThat(failures.get(0)).isInstanceOf(AppointmentConflictException.class);

        // Verify database state: exactly 1 appointment persisted with PENDING status
        List<AppointmentJpaEntity> persisted = appointmentJpaRepository.findAll();
        assertThat(persisted).hasSize(1);
        AppointmentJpaEntity winner = persisted.get(0);
        assertThat(winner.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(winner.getStaffId()).isEqualTo(doctor1.getId());
        assertThat(winner.getServiceNameSnapshot()).isEqualTo("Cardio Exam");
        assertThat(winner.getDurationMinutesSnapshot()).isEqualTo(30);
        assertThat(winner.getFeeAmountSnapshot()).isEqualByComparingTo("750.00");

        // Verify creation audit row exists only for winner
        assertThat(appointmentStatusHistoryJpaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-015: Overlapping but non-identical intervals (10:00-10:30 vs 10:15-10:45) serialize and fail second request")
    void concurrentBooking_overlappingIntervals_onlyOneSucceeds() throws Exception {
        BookingCommand cmd1 = new BookingCommand(patient1.getId(), doctor1.getId(), branchA.getId(), branchServiceA.getId(), targetDate, LocalTime.of(10, 0));
        BookingCommand cmd2 = new BookingCommand(patient2.getId(), doctor1.getId(), branchA.getId(), branchServiceA.getId(), targetDate, LocalTime.of(10, 15));

        List<Appointment> successes = new CopyOnWriteArrayList<>();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch doneLatch = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            executor.submit(() -> {
                try {
                    authenticateAs(patient1);
                    barrier.await(5, TimeUnit.SECONDS);
                    successes.add(bookingService.bookAppointment(cmd1));
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    SecurityContextHolder.clearContext();
                    doneLatch.countDown();
                }
            });

            executor.submit(() -> {
                try {
                    authenticateAs(patient2);
                    barrier.await(5, TimeUnit.SECONDS);
                    successes.add(bookingService.bookAppointment(cmd2));
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    SecurityContextHolder.clearContext();
                    doneLatch.countDown();
                }
            });

            boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
            assertThat(finished).isTrue();
        } finally {
            executor.shutdownNow();
        }

        assertThat(successes).hasSize(1);
        assertThat(failures).hasSize(1);
        assertThat(failures.get(0)).isInstanceOf(AppointmentConflictException.class);
        assertThat(appointmentJpaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-014: Adjacent intervals (09:00-09:30 and 09:30-10:00) both succeed without conflict")
    void concurrentBooking_adjacentSlots_bothSucceed() throws Exception {
        BookingCommand cmd1 = new BookingCommand(patient1.getId(), doctor1.getId(), branchA.getId(), branchServiceA.getId(), targetDate, LocalTime.of(9, 0));
        BookingCommand cmd2 = new BookingCommand(patient2.getId(), doctor1.getId(), branchA.getId(), branchServiceA.getId(), targetDate, LocalTime.of(9, 30));

        List<Appointment> successes = new CopyOnWriteArrayList<>();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch doneLatch = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            executor.submit(() -> {
                try {
                    authenticateAs(patient1);
                    barrier.await(5, TimeUnit.SECONDS);
                    successes.add(bookingService.bookAppointment(cmd1));
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    SecurityContextHolder.clearContext();
                    doneLatch.countDown();
                }
            });

            executor.submit(() -> {
                try {
                    authenticateAs(patient2);
                    barrier.await(5, TimeUnit.SECONDS);
                    successes.add(bookingService.bookAppointment(cmd2));
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    SecurityContextHolder.clearContext();
                    doneLatch.countDown();
                }
            });

            boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
            assertThat(finished).isTrue();
        } finally {
            executor.shutdownNow();
        }

        assertThat(failures).isEmpty();
        assertThat(successes).hasSize(2);
        assertThat(appointmentJpaRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("TC-015: Same doctor at two DIFFERENT branches at the same time: only one succeeds (global staff lock)")
    void concurrentBooking_sameStaffDifferentBranches_onlyOneSucceeds() throws Exception {
        LocalTime slotTime = LocalTime.of(11, 0);
        BookingCommand cmdBranchA = new BookingCommand(patient1.getId(), doctor1.getId(), branchA.getId(), branchServiceA.getId(), targetDate, slotTime);
        BookingCommand cmdBranchB = new BookingCommand(patient2.getId(), doctor1.getId(), branchB.getId(), branchServiceB.getId(), targetDate, slotTime);

        List<Appointment> successes = new CopyOnWriteArrayList<>();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch doneLatch = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            executor.submit(() -> {
                try {
                    authenticateAs(patient1);
                    barrier.await(5, TimeUnit.SECONDS);
                    successes.add(bookingService.bookAppointment(cmdBranchA));
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    SecurityContextHolder.clearContext();
                    doneLatch.countDown();
                }
            });

            executor.submit(() -> {
                try {
                    authenticateAs(patient2);
                    barrier.await(5, TimeUnit.SECONDS);
                    successes.add(bookingService.bookAppointment(cmdBranchB));
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    SecurityContextHolder.clearContext();
                    doneLatch.countDown();
                }
            });

            boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
            assertThat(finished).isTrue();
        } finally {
            executor.shutdownNow();
        }

        assertThat(successes).hasSize(1);
        assertThat(failures).hasSize(1);
        assertThat(failures.get(0)).isInstanceOf(AppointmentConflictException.class);
        assertThat(appointmentJpaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-015: Different doctors at the SAME time: both succeed concurrently without global serialization")
    void concurrentBooking_differentStaffSameTime_bothSucceed() throws Exception {
        LocalTime slotTime = LocalTime.of(14, 0);
        BookingCommand cmdDoc1 = new BookingCommand(patient1.getId(), doctor1.getId(), branchA.getId(), branchServiceA.getId(), targetDate, slotTime);
        BookingCommand cmdDoc2 = new BookingCommand(patient2.getId(), doctor2.getId(), branchA.getId(), branchServiceA.getId(), targetDate, slotTime);

        List<Appointment> successes = new CopyOnWriteArrayList<>();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch doneLatch = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            executor.submit(() -> {
                try {
                    authenticateAs(patient1);
                    barrier.await(5, TimeUnit.SECONDS);
                    successes.add(bookingService.bookAppointment(cmdDoc1));
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    SecurityContextHolder.clearContext();
                    doneLatch.countDown();
                }
            });

            executor.submit(() -> {
                try {
                    authenticateAs(patient2);
                    barrier.await(5, TimeUnit.SECONDS);
                    successes.add(bookingService.bookAppointment(cmdDoc2));
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    SecurityContextHolder.clearContext();
                    doneLatch.countDown();
                }
            });

            boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
            assertThat(finished).isTrue();
        } finally {
            executor.shutdownNow();
        }

        assertThat(failures).isEmpty();
        assertThat(successes).hasSize(2);
        assertThat(appointmentJpaRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("TC-029 / FR-029: Same patient booking two different doctors at the same time: only one succeeds (patient lock)")
    void concurrentBooking_samePatientDifferentStaff_onlyOneSucceeds() throws Exception {
        LocalTime slotTime = LocalTime.of(15, 0);
        BookingCommand cmd1 = new BookingCommand(patient1.getId(), doctor1.getId(), branchA.getId(), branchServiceA.getId(), targetDate, slotTime);
        BookingCommand cmd2 = new BookingCommand(patient1.getId(), doctor2.getId(), branchA.getId(), branchServiceA.getId(), targetDate, slotTime);

        List<Appointment> successes = new CopyOnWriteArrayList<>();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch doneLatch = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            executor.submit(() -> {
                try {
                    authenticateAs(patient1);
                    barrier.await(5, TimeUnit.SECONDS);
                    successes.add(bookingService.bookAppointment(cmd1));
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    SecurityContextHolder.clearContext();
                    doneLatch.countDown();
                }
            });

            executor.submit(() -> {
                try {
                    authenticateAs(patient1);
                    barrier.await(5, TimeUnit.SECONDS);
                    successes.add(bookingService.bookAppointment(cmd2));
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    SecurityContextHolder.clearContext();
                    doneLatch.countDown();
                }
            });

            boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
            assertThat(finished).isTrue();
        } finally {
            executor.shutdownNow();
        }

        assertThat(successes).hasSize(1);
        assertThat(failures).hasSize(1);
        assertThat(failures.get(0)).isInstanceOf(PatientOverlapException.class);
        assertThat(appointmentJpaRepository.count()).isEqualTo(1);
    }
}
