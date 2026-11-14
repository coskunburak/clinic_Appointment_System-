package com.example.clinic;

import com.example.clinic.booking.application.AppointmentConflictException;
import com.example.clinic.booking.application.BookingCommand;
import com.example.clinic.booking.application.BookingService;
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
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint S09 five-way concurrent booking stress verification (WP-S09-02, FR-015, NFR-004).
 * Intentionally runs without an outer test transaction so each worker acquires a separate PostgreSQL transaction.
 */
class ConcurrentBookingIT extends AbstractIntegrationTest {

    private static final int CONCURRENT_PATIENTS = 5;

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
    private Department dept;
    private BranchOpeningHours openingHours;
    private ServiceDefinition serviceDef;
    private BranchService branchService;
    private UserAccount doctorUser;
    private StaffProfile doctor;
    private Membership doctorMembership;
    private StaffAssignment assignment;
    private StaffServiceEligibility eligibility;
    private WeeklyAvailability weeklyAvailability;
    private final List<UserAccount> patientUsers = new ArrayList<>();
    private final List<PatientProfile> patients = new ArrayList<>();
    private final List<Membership> patientMemberships = new ArrayList<>();
    private LocalDate targetDate;

    @BeforeEach
    void setUpFixtures() {
        patientUsers.clear();
        patients.clear();
        patientMemberships.clear();

        targetDate = LocalDate.now().plusWeeks(3);
        while (targetDate.getDayOfWeek() != DayOfWeek.WEDNESDAY) {
            targetDate = targetDate.plusDays(1);
        }

        org = organizationRepository.saveAndFlush(new Organization("Five-Way Stress Health Group " + UUID.randomUUID()));
        branch = branchRepository.saveAndFlush(new Branch(org, "Stress Test Branch", "Europe/Istanbul", "Kadıköy"));
        dept = departmentRepository.saveAndFlush(new Department(branch, "Kardiyoloji"));
        openingHours = branchOpeningHoursRepository.saveAndFlush(
                new BranchOpeningHours(branch, (short) targetDate.getDayOfWeek().getValue(), LocalTime.of(8, 0), LocalTime.of(18, 0))
        );

        serviceDef = serviceDefinitionRepository.saveAndFlush(
                new ServiceDefinition(org, "Kardiyoloji Muayene " + UUID.randomUUID(), "Eşzamanlılık testi")
        );
        branchService = branchServiceRepository.saveAndFlush(new BranchService(
                org, branch, serviceDef, 30, Money.of(new BigDecimal("1500.00"), Currency.getInstance("TRY"))
        ));

        doctorUser = userAccountRepository.saveAndFlush(
                new UserAccount("stress.doc." + UUID.randomUUID() + "@clinic.test", "$2a$12$testHashPlaceholder0000000000000000000000000000000")
        );
        doctorMembership = membershipRepository.saveAndFlush(new Membership(doctorUser, org, branch, Role.ROLE_STAFF));
        doctor = staffProfileRepository.saveAndFlush(
                new StaffProfile(doctorUser, "Dr. Stres Hekim", "+905550001000", StaffType.DOCTOR, "Kardiyoloji")
        );
        assignment = staffAssignmentRepository.saveAndFlush(new StaffAssignment(doctor, branch, dept));
        eligibility = staffServiceEligibilityRepository.saveAndFlush(new StaffServiceEligibility(assignment, branchService));
        weeklyAvailability = weeklyAvailabilityRepository.saveAndFlush(
                new WeeklyAvailability(assignment, (short) targetDate.getDayOfWeek().getValue(), LocalTime.of(9, 0), LocalTime.of(17, 0))
        );

        for (int i = 0; i < CONCURRENT_PATIENTS; i++) {
            UserAccount patUser = userAccountRepository.saveAndFlush(
                    new UserAccount("stress.pat." + i + "." + UUID.randomUUID() + "@clinic.test", "$2a$12$testHashPlaceholder0000000000000000000000000000000")
            );
            Membership membership = membershipRepository.saveAndFlush(new Membership(patUser, org, null, Role.ROLE_PATIENT));
            PatientProfile profile = patientProfileRepository.saveAndFlush(
                    new PatientProfile(patUser, "Hasta " + (i + 1), "+90555000200" + i)
            );
            patientUsers.add(patUser);
            patientMemberships.add(membership);
            patients.add(profile);
        }
    }

    @AfterEach
    void tearDownFixtures() {
        SecurityContextHolder.clearContext();

        List<UUID> branchIds = branch != null ? List.of(branch.getId()) : List.of();
        List<UUID> apptIds = branchIds.isEmpty() ? List.of() : appointmentJpaRepository.findIdsByBranchIdIn(branchIds);
        if (!apptIds.isEmpty()) {
            List<UUID> historyIds = appointmentStatusHistoryJpaRepository.findIdsByAppointmentIdIn(apptIds);
            if (!historyIds.isEmpty()) {
                appointmentStatusHistoryJpaRepository.deleteAllByIdInBatch(historyIds);
            }
            appointmentJpaRepository.deleteAllByIdInBatch(apptIds);
        }

        if (weeklyAvailability != null) {
            weeklyAvailabilityRepository.deleteAllByIdInBatch(List.of(weeklyAvailability.getId()));
        }
        if (eligibility != null) {
            staffServiceEligibilityRepository.deleteAllByIdInBatch(List.of(eligibility.getId()));
        }
        if (assignment != null) {
            staffAssignmentRepository.deleteAllByIdInBatch(List.of(assignment.getId()));
        }
        if (branchService != null) {
            branchServiceRepository.deleteAllByIdInBatch(List.of(branchService.getId()));
        }
        if (serviceDef != null) {
            serviceDefinitionRepository.deleteAllByIdInBatch(List.of(serviceDef.getId()));
        }
        if (dept != null) {
            departmentRepository.deleteAllByIdInBatch(List.of(dept.getId()));
        }
        if (openingHours != null) {
            branchOpeningHoursRepository.deleteAllByIdInBatch(List.of(openingHours.getId()));
        }

        List<UUID> membershipIds = new ArrayList<>();
        if (doctorMembership != null) {
            membershipIds.add(doctorMembership.getId());
        }
        patientMemberships.forEach(m -> membershipIds.add(m.getId()));
        if (!membershipIds.isEmpty()) {
            membershipRepository.deleteAllByIdInBatch(membershipIds);
        }

        List<UUID> patIds = patients.stream().map(PatientProfile::getId).toList();
        if (!patIds.isEmpty()) {
            patientProfileRepository.deleteAllByIdInBatch(patIds);
        }
        if (doctor != null) {
            staffProfileRepository.deleteAllByIdInBatch(List.of(doctor.getId()));
        }

        List<UUID> userIds = new ArrayList<>();
        if (doctorUser != null) {
            userIds.add(doctorUser.getId());
        }
        patientUsers.forEach(u -> userIds.add(u.getId()));
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
    @DisplayName("WP-S09-02: Five simultaneous patients targeting the exact same doctor slot produce 1 PENDING booking and 4 conflict failures")
    void fiveConcurrentThreadsCompetingForSameSlotProduceSingleBookingAndFourConflicts() throws Exception {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();

        LocalTime contendedSlot = LocalTime.of(10, 0);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_PATIENTS);
        CountDownLatch readyLatch = new CountDownLatch(CONCURRENT_PATIENTS);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_PATIENTS);

        List<Appointment> successes = new CopyOnWriteArrayList<>();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        try {
            for (int i = 0; i < CONCURRENT_PATIENTS; i++) {
                final UserAccount patientUser = patientUsers.get(i);
                final PatientProfile patientProfile = patients.get(i);
                executor.submit(() -> {
                    try {
                        authenticateAs(patientUser);
                        readyLatch.countDown();
                        if (!startGate.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Start gate timed out");
                        }
                        BookingCommand command = new BookingCommand(
                                patientProfile.getId(),
                                doctor.getId(),
                                branch.getId(),
                                branchService.getId(),
                                targetDate,
                                contendedSlot
                        );
                        successes.add(bookingService.bookAppointment(command));
                    } catch (Throwable t) {
                        failures.add(t);
                    } finally {
                        SecurityContextHolder.clearContext();
                        doneLatch.countDown();
                    }
                });
            }

            assertThat(readyLatch.await(10, TimeUnit.SECONDS)).isTrue();
            startGate.countDown();
            assertThat(doneLatch.await(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }

        assertThat(successes).hasSize(1);
        assertThat(failures).hasSize(4);
        assertThat(failures).allSatisfy(err -> assertThat(err).isInstanceOf(AppointmentConflictException.class));

        Appointment winner = successes.get(0);
        assertThat(winner.getStatus()).isEqualTo(AppointmentStatus.PENDING);

        List<AppointmentJpaEntity> persistedForBranch = appointmentJpaRepository.findByBranchId(branch.getId());
        assertThat(persistedForBranch).hasSize(1);
        assertThat(persistedForBranch.get(0).getId()).isEqualTo(winner.getId());
        assertThat(persistedForBranch.get(0).getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(persistedForBranch.get(0).getServiceNameSnapshot()).isEqualTo(serviceDef.getName());

        List<AppointmentStatusHistoryJpaEntity> history =
                appointmentStatusHistoryJpaRepository.findByAppointmentIdOrderByOccurredAtAsc(winner.getId());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getFromStatus()).isNull();
        assertThat(history.get(0).getToStatus()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    @DisplayName("WP-S09-02: Five simultaneous patients targeting five distinct non-overlapping slots for the same doctor all succeed")
    void fiveConcurrentThreadsForDistinctSlotsAllSucceedWithoutErrors() throws Exception {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_PATIENTS);
        CountDownLatch readyLatch = new CountDownLatch(CONCURRENT_PATIENTS);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_PATIENTS);

        List<Appointment> successes = new CopyOnWriteArrayList<>();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        try {
            for (int i = 0; i < CONCURRENT_PATIENTS; i++) {
                final int index = i;
                final UserAccount patientUser = patientUsers.get(i);
                final PatientProfile patientProfile = patients.get(i);
                final LocalTime distinctSlot = LocalTime.of(9, 0).plusMinutes(index * 30L);

                executor.submit(() -> {
                    try {
                        authenticateAs(patientUser);
                        readyLatch.countDown();
                        if (!startGate.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Start gate timed out");
                        }
                        BookingCommand command = new BookingCommand(
                                patientProfile.getId(),
                                doctor.getId(),
                                branch.getId(),
                                branchService.getId(),
                                targetDate,
                                distinctSlot
                        );
                        successes.add(bookingService.bookAppointment(command));
                    } catch (Throwable t) {
                        failures.add(t);
                    } finally {
                        SecurityContextHolder.clearContext();
                        doneLatch.countDown();
                    }
                });
            }

            assertThat(readyLatch.await(10, TimeUnit.SECONDS)).isTrue();
            startGate.countDown();
            assertThat(doneLatch.await(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }

        assertThat(failures).isEmpty();
        assertThat(successes).hasSize(CONCURRENT_PATIENTS);
        assertThat(appointmentJpaRepository.findByBranchId(branch.getId())).hasSize(CONCURRENT_PATIENTS);
    }

    private void authenticateAs(UserAccount patientUser) {
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
