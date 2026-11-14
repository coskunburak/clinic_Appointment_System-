package com.example.clinic.common.infrastructure;

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
import com.example.clinic.common.domain.TimeInterval;
import com.example.clinic.identity.application.AccountCredentialService;
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
import com.example.clinic.staff.domain.AvailabilityException;
import com.example.clinic.staff.domain.AvailabilityExceptionKind;
import com.example.clinic.staff.domain.StaffAssignment;
import com.example.clinic.staff.domain.StaffServiceEligibility;
import com.example.clinic.staff.domain.WeeklyAvailability;
import com.example.clinic.staff.infrastructure.AvailabilityExceptionRepository;
import com.example.clinic.staff.infrastructure.StaffAssignmentRepository;
import com.example.clinic.staff.infrastructure.StaffServiceEligibilityRepository;
import com.example.clinic.staff.infrastructure.WeeklyAvailabilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Transactional, idempotent seeding service creating a coherent demonstration clinic environment (FR-034, NFR-005).
 * Executes exclusively inside {@code dev} profile after {@link DemoEnvironmentGuard} verification.
 */
@Service
@Profile("dev")
public class DemoDataSeedService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeedService.class);

    private static final List<AppointmentStatus> BLOCKING_STATUSES = List.of(
            AppointmentStatus.PENDING,
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.COMPLETED
    );

    private static final Currency TRY_CURRENCY = Currency.getInstance(DemoDataCatalog.CURRENCY_TRY);

    private final DemoEnvironmentGuard guard;
    private final DemoDataProperties properties;
    private final AccountCredentialService accountCredentialService;
    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;
    private final BranchOpeningHoursRepository branchOpeningHoursRepository;
    private final DepartmentRepository departmentRepository;
    private final ServiceDefinitionRepository serviceDefinitionRepository;
    private final BranchServiceRepository branchServiceRepository;
    private final UserAccountRepository userAccountRepository;
    private final MembershipRepository membershipRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final StaffServiceEligibilityRepository staffServiceEligibilityRepository;
    private final WeeklyAvailabilityRepository weeklyAvailabilityRepository;
    private final AvailabilityExceptionRepository availabilityExceptionRepository;
    private final AppointmentJpaRepository appointmentJpaRepository;
    private final AppointmentStatusHistoryJpaRepository appointmentStatusHistoryJpaRepository;
    private final Clock clock;

    public DemoDataSeedService(DemoEnvironmentGuard guard,
                               DemoDataProperties properties,
                               AccountCredentialService accountCredentialService,
                               OrganizationRepository organizationRepository,
                               BranchRepository branchRepository,
                               BranchOpeningHoursRepository branchOpeningHoursRepository,
                               DepartmentRepository departmentRepository,
                               ServiceDefinitionRepository serviceDefinitionRepository,
                               BranchServiceRepository branchServiceRepository,
                               UserAccountRepository userAccountRepository,
                               MembershipRepository membershipRepository,
                               PatientProfileRepository patientProfileRepository,
                               StaffProfileRepository staffProfileRepository,
                               StaffAssignmentRepository staffAssignmentRepository,
                               StaffServiceEligibilityRepository staffServiceEligibilityRepository,
                               WeeklyAvailabilityRepository weeklyAvailabilityRepository,
                               AvailabilityExceptionRepository availabilityExceptionRepository,
                               AppointmentJpaRepository appointmentJpaRepository,
                               AppointmentStatusHistoryJpaRepository appointmentStatusHistoryJpaRepository,
                               Clock clock) {
        this.guard = Objects.requireNonNull(guard, "guard must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.accountCredentialService = Objects.requireNonNull(accountCredentialService, "accountCredentialService must not be null");
        this.organizationRepository = Objects.requireNonNull(organizationRepository, "organizationRepository must not be null");
        this.branchRepository = Objects.requireNonNull(branchRepository, "branchRepository must not be null");
        this.branchOpeningHoursRepository = Objects.requireNonNull(branchOpeningHoursRepository, "branchOpeningHoursRepository must not be null");
        this.departmentRepository = Objects.requireNonNull(departmentRepository, "departmentRepository must not be null");
        this.serviceDefinitionRepository = Objects.requireNonNull(serviceDefinitionRepository, "serviceDefinitionRepository must not be null");
        this.branchServiceRepository = Objects.requireNonNull(branchServiceRepository, "branchServiceRepository must not be null");
        this.userAccountRepository = Objects.requireNonNull(userAccountRepository, "userAccountRepository must not be null");
        this.membershipRepository = Objects.requireNonNull(membershipRepository, "membershipRepository must not be null");
        this.patientProfileRepository = Objects.requireNonNull(patientProfileRepository, "patientProfileRepository must not be null");
        this.staffProfileRepository = Objects.requireNonNull(staffProfileRepository, "staffProfileRepository must not be null");
        this.staffAssignmentRepository = Objects.requireNonNull(staffAssignmentRepository, "staffAssignmentRepository must not be null");
        this.staffServiceEligibilityRepository = Objects.requireNonNull(staffServiceEligibilityRepository, "staffServiceEligibilityRepository must not be null");
        this.weeklyAvailabilityRepository = Objects.requireNonNull(weeklyAvailabilityRepository, "weeklyAvailabilityRepository must not be null");
        this.availabilityExceptionRepository = Objects.requireNonNull(availabilityExceptionRepository, "availabilityExceptionRepository must not be null");
        this.appointmentJpaRepository = Objects.requireNonNull(appointmentJpaRepository, "appointmentJpaRepository must not be null");
        this.appointmentStatusHistoryJpaRepository = Objects.requireNonNull(appointmentStatusHistoryJpaRepository, "appointmentStatusHistoryJpaRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Populates the development database with deterministic clinic demo records.
     * Existing unrelated data and previously seeded demo records remain intact without duplication.
     */
    @Transactional
    public void seedDemoData() {
        guard.assertSafeForSeeding();

        Instant now = clock.instant();
        Organization org = ensureOrganization(now);

        Branch merkez = ensureBranch(
                DemoDataCatalog.BRANCH_MERKEZ_ID,
                org,
                DemoDataCatalog.BRANCH_MERKEZ_NAME,
                DemoDataCatalog.BRANCH_ZONE_ISTANBUL,
                DemoDataCatalog.BRANCH_MERKEZ_ADDRESS,
                now
        );
        Branch sahil = ensureBranch(
                DemoDataCatalog.BRANCH_SAHIL_ID,
                org,
                DemoDataCatalog.BRANCH_SAHIL_NAME,
                DemoDataCatalog.BRANCH_ZONE_ISTANBUL,
                DemoDataCatalog.BRANCH_SAHIL_ADDRESS,
                now
        );

        seedOpeningHours(merkez, true);
        seedOpeningHours(sahil, false);

        Department merkezKardiyoloji = ensureDepartment(DemoDataCatalog.DEPT_MERKEZ_KARDIYOLOJI_ID, merkez, DemoDataCatalog.DEPT_KARDIYOLOJI, now);
        ensureDepartment(DemoDataCatalog.DEPT_MERKEZ_RADYOLOJI_ID, merkez, DemoDataCatalog.DEPT_RADYOLOJI, now);
        ensureDepartment(DemoDataCatalog.DEPT_MERKEZ_DAHILIYE_ID, merkez, DemoDataCatalog.DEPT_DAHILIYE, now);

        Department sahilDahiliye = ensureDepartment(DemoDataCatalog.DEPT_SAHIL_DAHILIYE_ID, sahil, DemoDataCatalog.DEPT_DAHILIYE, now);
        ensureDepartment(DemoDataCatalog.DEPT_SAHIL_LABORATUVAR_ID, sahil, DemoDataCatalog.DEPT_LABORATUVAR, now);

        ServiceDefinition genelMuayene = ensureServiceDefinition(
                DemoDataCatalog.SERVICE_DEF_GENEL_MUAYENE_ID,
                org,
                DemoDataCatalog.SERVICE_GENEL_MUAYENE,
                "Genel dahiliye ve ön değerlendirme muayenesi.",
                now
        );
        ServiceDefinition ekoCekimi = ensureServiceDefinition(
                DemoDataCatalog.SERVICE_DEF_EKO_CEKIMI_ID,
                org,
                DemoDataCatalog.SERVICE_EKO_CEKIMI,
                "Transtorasik ekokardiyografi kalp ultrasonografisi.",
                now
        );
        ServiceDefinition akcigerGrafisi = ensureServiceDefinition(
                DemoDataCatalog.SERVICE_DEF_RADYOLOJI_AKCIGER_ID,
                org,
                DemoDataCatalog.SERVICE_RADYOLOJI_AKCIGER,
                "Dijital PA akciğer radyografi çekimi ve raporlama.",
                now
        );
        ServiceDefinition kanTahlili = ensureServiceDefinition(
                DemoDataCatalog.SERVICE_DEF_KAN_TAHLILI_ID,
                org,
                DemoDataCatalog.SERVICE_KAN_TAHLILI,
                "Tam kan sayımı ve temel biyokimya laboratuvar paneli.",
                now
        );

        BranchService merkezGenel = ensureBranchService(
                DemoDataCatalog.BRANCH_SERVICE_MERKEZ_GENEL_ID,
                merkez, genelMuayene, DemoDataCatalog.DURATION_GENEL_MUAYENE, DemoDataCatalog.FEE_GENEL_MUAYENE, now
        );
        BranchService merkezEko = ensureBranchService(
                DemoDataCatalog.BRANCH_SERVICE_MERKEZ_EKO_ID,
                merkez, ekoCekimi, DemoDataCatalog.DURATION_EKO_CEKIMI, DemoDataCatalog.FEE_EKO_CEKIMI, now
        );
        BranchService merkezRadyoloji = ensureBranchService(
                DemoDataCatalog.BRANCH_SERVICE_MERKEZ_RADYOLOJI_ID,
                merkez, akcigerGrafisi, DemoDataCatalog.DURATION_RADYOLOJI_AKCIGER, DemoDataCatalog.FEE_RADYOLOJI_AKCIGER, now
        );
        BranchService sahilGenel = ensureBranchService(
                DemoDataCatalog.BRANCH_SERVICE_SAHIL_GENEL_ID,
                sahil, genelMuayene, DemoDataCatalog.DURATION_GENEL_MUAYENE, DemoDataCatalog.FEE_GENEL_MUAYENE, now
        );
        BranchService sahilKan = ensureBranchService(
                DemoDataCatalog.BRANCH_SERVICE_SAHIL_KAN_ID,
                sahil, kanTahlili, DemoDataCatalog.DURATION_KAN_TAHLILI, DemoDataCatalog.FEE_KAN_TAHLILI, now
        );

        DemoDataProperties.Credentials creds = properties.getCredentials();
        UserAccount adminUser = ensureUserAccount(DemoDataCatalog.USER_ADMIN_ID, DemoDataCatalog.ADMIN_EMAIL, creds.getAdminPassword(), now);
        UserAccount ahmetUser = ensureUserAccount(DemoDataCatalog.USER_DOCTOR_AHMET_ID, DemoDataCatalog.DOCTOR_AHMET_EMAIL, creds.getDoctorPassword(), now);
        UserAccount ayseUser = ensureUserAccount(DemoDataCatalog.USER_DOCTOR_AYSE_ID, DemoDataCatalog.DOCTOR_AYSE_EMAIL, creds.getDoctorPassword(), now);
        UserAccount aliUser = ensureUserAccount(DemoDataCatalog.USER_PATIENT_ALI_ID, DemoDataCatalog.PATIENT_ALI_EMAIL, creds.getPatientPassword(), now);
        UserAccount zeynepUser = ensureUserAccount(DemoDataCatalog.USER_PATIENT_ZEYNEP_ID, DemoDataCatalog.PATIENT_ZEYNEP_EMAIL, creds.getPatientPassword(), now);

        ensureMembership(DemoDataCatalog.MEMBERSHIP_ADMIN_ID, adminUser, org, null, Role.ROLE_ADMIN, now);
        ensureMembership(DemoDataCatalog.MEMBERSHIP_AHMET_ID, ahmetUser, org, merkez, Role.ROLE_STAFF, now);
        ensureMembership(DemoDataCatalog.MEMBERSHIP_AYSE_ID, ayseUser, org, sahil, Role.ROLE_STAFF, now);
        ensureMembership(DemoDataCatalog.MEMBERSHIP_ALI_ID, aliUser, org, null, Role.ROLE_PATIENT, now);
        ensureMembership(DemoDataCatalog.MEMBERSHIP_ZEYNEP_ID, zeynepUser, org, null, Role.ROLE_PATIENT, now);

        StaffProfile ahmetProfile = ensureStaffProfile(
                DemoDataCatalog.STAFF_AHMET_ID, ahmetUser, "Dr. Ahmet Kaya", "+905551000010", "Kardiyoloji", now
        );
        StaffProfile ayseProfile = ensureStaffProfile(
                DemoDataCatalog.STAFF_AYSE_ID, ayseUser, "Dr. Ayşe Demir", "+905551000011", "Dahiliye", now
        );

        PatientProfile aliProfile = ensurePatientProfile(
                DemoDataCatalog.PATIENT_ALI_ID, aliUser, "Ali Yılmaz", "+905551000001", now
        );
        PatientProfile zeynepProfile = ensurePatientProfile(
                DemoDataCatalog.PATIENT_ZEYNEP_ID, zeynepUser, "Zeynep Kaya", "+905551000002", now
        );

        StaffAssignment ahmetAssignment = ensureStaffAssignment(
                DemoDataCatalog.ASSIGNMENT_AHMET_MERKEZ_ID, ahmetProfile, merkez, merkezKardiyoloji, now
        );
        StaffAssignment ayseAssignment = ensureStaffAssignment(
                DemoDataCatalog.ASSIGNMENT_AYSE_SAHIL_ID, ayseProfile, sahil, sahilDahiliye, now
        );

        ensureEligibility("ahmet:merkez-genel", ahmetAssignment, merkezGenel, now);
        ensureEligibility("ahmet:merkez-eko", ahmetAssignment, merkezEko, now);
        ensureEligibility("ahmet:merkez-radyoloji", ahmetAssignment, merkezRadyoloji, now);
        ensureEligibility("ayse:sahil-genel", ayseAssignment, sahilGenel, now);
        ensureEligibility("ayse:sahil-kan", ayseAssignment, sahilKan, now);

        seedWeeklyAvailability(ahmetAssignment, true, now);
        seedWeeklyAvailability(ayseAssignment, false, now);

        ZoneId istanbulZone = ZoneId.of(DemoDataCatalog.BRANCH_ZONE_ISTANBUL);
        LocalDate today = LocalDate.now(clock.withZone(istanbulZone));
        LocalDate pastWorkday = resolvePreviousWorkday(today);
        LocalDate nextWorkday = resolveNextWorkday(today);

        seedAvailabilityExceptions(ahmetAssignment, ayseAssignment, nextWorkday, now);
        seedAppointments(
                istanbulZone,
                pastWorkday,
                nextWorkday,
                ahmetUser,
                ayseUser,
                aliUser,
                zeynepUser,
                ahmetProfile,
                ayseProfile,
                aliProfile,
                zeynepProfile,
                merkez,
                sahil,
                merkezGenel,
                merkezEko,
                sahilGenel,
                sahilKan
        );

        log.info("Demo data initialization completed idempotently for organization '{}'.", org.getName());
    }

    private Organization ensureOrganization(Instant now) {
        return organizationRepository.findById(DemoDataCatalog.ORG_ID)
                .or(() -> organizationRepository.findByName(DemoDataCatalog.ORG_NAME))
                .orElseGet(() -> organizationRepository.saveAndFlush(
                        new Organization(DemoDataCatalog.ORG_ID, DemoDataCatalog.ORG_NAME, true, now)
                ));
    }

    private Branch ensureBranch(UUID id, Organization org, String name, String timeZone, String address, Instant now) {
        return branchRepository.findById(id)
                .or(() -> branchRepository.findByOrganizationId(org.getId()).stream()
                        .filter(b -> name.equals(b.getName()))
                        .findFirst())
                .orElseGet(() -> branchRepository.saveAndFlush(
                        new Branch(id, org, name, timeZone, address, true, now)
                ));
    }

    private void seedOpeningHours(Branch branch, boolean openOnSaturday) {
        String branchKey = branch.getId().toString();
        for (short day = 1; day <= 5; day++) {
            LocalTime start = openOnSaturday ? LocalTime.of(8, 30) : LocalTime.of(9, 0);
            LocalTime end = LocalTime.of(18, 0);
            ensureOpeningHour(branch, day, start, end, branchKey);
        }
        if (openOnSaturday) {
            ensureOpeningHour(branch, (short) DayOfWeek.SATURDAY.getValue(), LocalTime.of(9, 0), LocalTime.of(14, 0), branchKey);
        }
    }

    private void ensureOpeningHour(Branch branch, short weekday, LocalTime startTime, LocalTime endTime, String branchKey) {
        UUID hourId = DemoDataCatalog.id("opening-hour:" + branchKey + ":" + weekday);
        if (branchOpeningHoursRepository.existsById(hourId)) {
            return;
        }
        boolean alreadyExists = branchOpeningHoursRepository.findByBranchIdAndWeekday(branch.getId(), weekday).stream()
                .anyMatch(h -> startTime.equals(h.getStartTime()) && endTime.equals(h.getEndTime()));
        if (!alreadyExists) {
            branchOpeningHoursRepository.saveAndFlush(new BranchOpeningHours(hourId, branch, weekday, startTime, endTime));
        }
    }

    private Department ensureDepartment(UUID id, Branch branch, String name, Instant now) {
        return departmentRepository.findById(id)
                .or(() -> departmentRepository.findByBranchIdAndName(branch.getId(), name))
                .orElseGet(() -> departmentRepository.saveAndFlush(new Department(id, branch, name, true, now)));
    }

    private ServiceDefinition ensureServiceDefinition(UUID id, Organization org, String name, String description, Instant now) {
        return serviceDefinitionRepository.findById(id)
                .or(() -> serviceDefinitionRepository.findByOrganizationIdAndName(org.getId(), name))
                .orElseGet(() -> serviceDefinitionRepository.saveAndFlush(
                        new ServiceDefinition(id, org, name, description, true, now)
                ));
    }

    private BranchService ensureBranchService(UUID id, Branch branch, ServiceDefinition serviceDefinition,
                                              int durationMinutes, BigDecimal feeAmount, Instant now) {
        return branchServiceRepository.findById(id)
                .or(() -> branchServiceRepository.findByBranchIdAndServiceDefinitionId(branch.getId(), serviceDefinition.getId()))
                .orElseGet(() -> branchServiceRepository.saveAndFlush(new BranchService(
                        id,
                        branch.getOrganization(),
                        branch,
                        serviceDefinition,
                        durationMinutes,
                        Money.of(feeAmount, TRY_CURRENCY),
                        true,
                        now
                )));
    }

    private UserAccount ensureUserAccount(UUID id, String rawEmail, String rawPassword, Instant now) {
        String normalizedEmail = accountCredentialService.normalizeEmail(rawEmail);
        return userAccountRepository.findById(id)
                .or(() -> userAccountRepository.findByEmail(normalizedEmail))
                .orElseGet(() -> {
                    String passwordHash = accountCredentialService.encodeVerifiedPassword(rawPassword);
                    return userAccountRepository.saveAndFlush(new UserAccount(id, normalizedEmail, passwordHash, true, now));
                });
    }

    private void ensureMembership(UUID id, UserAccount user, Organization org, Branch branch, Role role, Instant now) {
        if (membershipRepository.existsById(id)) {
            return;
        }
        UUID branchId = branch != null ? branch.getId() : null;
        boolean exists = membershipRepository.findByUserAccountIdAndRole(user.getId(), role).stream()
                .anyMatch(m -> m.getOrganization().getId().equals(org.getId())
                        && Objects.equals(m.getBranch() != null ? m.getBranch().getId() : null, branchId));
        if (!exists) {
            membershipRepository.saveAndFlush(new Membership(id, user, org, branch, role, now));
        }
    }

    private StaffProfile ensureStaffProfile(UUID id, UserAccount user, String fullName, String phone, String specialty, Instant now) {
        return staffProfileRepository.findById(id)
                .or(() -> staffProfileRepository.findByUserAccountId(user.getId()))
                .orElseGet(() -> staffProfileRepository.saveAndFlush(
                        new StaffProfile(id, user, fullName, phone, StaffType.DOCTOR, specialty, true, now)
                ));
    }

    private PatientProfile ensurePatientProfile(UUID id, UserAccount user, String fullName, String phone, Instant now) {
        return patientProfileRepository.findById(id)
                .or(() -> patientProfileRepository.findByUserAccountId(user.getId()))
                .orElseGet(() -> patientProfileRepository.saveAndFlush(
                        new PatientProfile(id, user, fullName, phone, now)
                ));
    }

    private StaffAssignment ensureStaffAssignment(UUID id, StaffProfile staff, Branch branch, Department department, Instant now) {
        return staffAssignmentRepository.findById(id)
                .or(() -> staffAssignmentRepository.findByStaffIdAndBranchIdAndDepartmentId(
                        staff.getId(), branch.getId(), department.getId()))
                .orElseGet(() -> staffAssignmentRepository.saveAndFlush(
                        new StaffAssignment(id, staff, branch, department, true, now)
                ));
    }

    private void ensureEligibility(String key, StaffAssignment assignment, BranchService branchService, Instant now) {
        UUID eligibilityId = DemoDataCatalog.id("eligibility:" + key);
        if (staffServiceEligibilityRepository.existsById(eligibilityId)) {
            return;
        }
        if (!staffServiceEligibilityRepository.existsByAssignmentIdAndBranchServiceId(assignment.getId(), branchService.getId())) {
            staffServiceEligibilityRepository.saveAndFlush(
                    new StaffServiceEligibility(eligibilityId, assignment, branchService, now)
            );
        }
    }

    private void seedWeeklyAvailability(StaffAssignment assignment, boolean includeSaturdayMorning, Instant now) {
        String key = assignment.getId().toString();
        for (short day = 1; day <= 5; day++) {
            ensureWeeklyAvailability(assignment, day, LocalTime.of(9, 0), LocalTime.of(17, 0), key, now);
        }
        if (includeSaturdayMorning) {
            ensureWeeklyAvailability(assignment, (short) DayOfWeek.SATURDAY.getValue(), LocalTime.of(9, 0), LocalTime.of(13, 0), key, now);
        }
    }

    private void ensureWeeklyAvailability(StaffAssignment assignment, short weekday,
                                          LocalTime startTime, LocalTime endTime, String key, Instant now) {
        UUID availId = DemoDataCatalog.id("weekly-avail:" + key + ":" + weekday);
        if (weeklyAvailabilityRepository.existsById(availId)) {
            return;
        }
        boolean exists = weeklyAvailabilityRepository.findByAssignmentIdAndWeekday(assignment.getId(), weekday).stream()
                .anyMatch(w -> startTime.equals(w.getStartTime()) && endTime.equals(w.getEndTime()));
        if (!exists) {
            weeklyAvailabilityRepository.saveAndFlush(new WeeklyAvailability(availId, assignment, weekday, startTime, endTime, now));
        }
    }

    private void seedAvailabilityExceptions(StaffAssignment ahmetAssignment,
                                            StaffAssignment ayseAssignment,
                                            LocalDate nextWorkday,
                                            Instant now) {
        LocalDate ahmetPartialDate = nextWorkday.plusDays(7);
        LocalDate ahmetFullLeaveDate = nextWorkday.plusDays(14);
        LocalDate ayseOvertimeDate = nextWorkday.plusDays(7);

        ensureException(
                DemoDataCatalog.EXCEPTION_AHMET_PARTIAL_LEAVE_ID,
                ahmetAssignment,
                ahmetPartialDate,
                AvailabilityExceptionKind.PARTIAL_LEAVE,
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                "Öğle arası klinik kurulu",
                now
        );
        ensureException(
                DemoDataCatalog.EXCEPTION_AYSE_OVERTIME_ID,
                ayseAssignment,
                ayseOvertimeDate,
                AvailabilityExceptionKind.OVERTIME,
                LocalTime.of(17, 0),
                LocalTime.of(18, 0),
                "Akşam polikliniği ek seansı",
                now
        );
        ensureException(
                DemoDataCatalog.EXCEPTION_AHMET_FULL_DAY_LEAVE_ID,
                ahmetAssignment,
                ahmetFullLeaveDate,
                AvailabilityExceptionKind.FULL_DAY_LEAVE,
                null,
                null,
                "Kardiyoloji eğitim semineri",
                now
        );
    }

    private void ensureException(UUID id, StaffAssignment assignment, LocalDate localDate,
                                 AvailabilityExceptionKind kind, LocalTime startTime, LocalTime endTime,
                                 String reason, Instant now) {
        if (availabilityExceptionRepository.existsById(id)) {
            return;
        }
        if (availabilityExceptionRepository.findByAssignmentIdAndLocalDate(assignment.getId(), localDate).isEmpty()) {
            availabilityExceptionRepository.saveAndFlush(
                    new AvailabilityException(id, assignment, localDate, kind, startTime, endTime, reason, now)
            );
        }
    }

    private void seedAppointments(ZoneId zone,
                                  LocalDate pastWorkday,
                                  LocalDate nextWorkday,
                                  UserAccount ahmetUser,
                                  UserAccount ayseUser,
                                  UserAccount aliUser,
                                  UserAccount zeynepUser,
                                  StaffProfile ahmetProfile,
                                  StaffProfile ayseProfile,
                                  PatientProfile aliProfile,
                                  PatientProfile zeynepProfile,
                                  Branch merkez,
                                  Branch sahil,
                                  BranchService merkezGenel,
                                  BranchService merkezEko,
                                  BranchService sahilGenel,
                                  BranchService sahilKan) {
        Instant now = clock.instant();

        // 1. Past COMPLETED appointment (Ali + Dr. Ahmet @ Merkez)
        seedCompletedAppointment(
                DemoDataCatalog.APPT_COMPLETED_AHMET_ID,
                aliProfile, aliUser, ahmetProfile, ahmetUser, merkez, merkezGenel,
                pastWorkday, LocalTime.of(9, 30), zone, now, "complete-ahmet"
        );

        // 2. Past COMPLETED appointment (Zeynep + Dr. Ayşe @ Sahil)
        seedCompletedAppointment(
                DemoDataCatalog.APPT_COMPLETED_AYSE_ID,
                zeynepProfile, zeynepUser, ayseProfile, ayseUser, sahil, sahilKan,
                pastWorkday, LocalTime.of(10, 30), zone, now, "complete-ayse"
        );

        // 3. Future CONFIRMED appointment (Ali + Dr. Ahmet @ Merkez)
        seedConfirmedAppointment(
                DemoDataCatalog.APPT_CONFIRMED_AHMET_ID,
                aliProfile, aliUser, ahmetProfile, ahmetUser, merkez, merkezGenel,
                nextWorkday, LocalTime.of(10, 0), zone, now
        );

        // 4. Future PENDING appointment (Zeynep + Dr. Ahmet @ Merkez)
        seedPendingAppointment(
                DemoDataCatalog.APPT_PENDING_AHMET_ID,
                zeynepProfile, zeynepUser, ahmetProfile, merkez, merkezEko,
                nextWorkday, LocalTime.of(11, 0), zone, now
        );

        // 5. Future PENDING appointment (Ali + Dr. Ayşe @ Sahil)
        seedPendingAppointment(
                DemoDataCatalog.APPT_PENDING_AYSE_ID,
                aliProfile, aliUser, ayseProfile, sahil, sahilKan,
                nextWorkday, LocalTime.of(14, 0), zone, now
        );

        // 6. Future REJECTED appointment (Zeynep + Dr. Ayşe @ Sahil)
        seedRejectedAppointment(
                DemoDataCatalog.APPT_REJECTED_AYSE_ID,
                zeynepProfile, zeynepUser, ayseProfile, ayseUser, sahil, sahilGenel,
                nextWorkday, LocalTime.of(15, 0), zone, now
        );

        // 7. Future CANCELLED appointment (Ali + Dr. Ahmet @ Merkez)
        seedCancelledAppointment(
                DemoDataCatalog.APPT_CANCELLED_AHMET_ID,
                aliProfile, aliUser, ahmetProfile, ahmetUser, merkez, merkezGenel,
                nextWorkday, LocalTime.of(15, 30), zone, now
        );
    }

    private void seedCompletedAppointment(UUID appointmentId, PatientProfile patient, UserAccount patientUser,
                                          StaffProfile staff, UserAccount staffUser, Branch branch, BranchService service,
                                          LocalDate date, LocalTime startTime, ZoneId zone, Instant now, String historyKey) {
        if (appointmentJpaRepository.existsById(appointmentId)) {
            return;
        }
        ServiceSnapshot snapshot = service.toSnapshot();
        Instant startAt = ZonedDateTime.of(date, startTime, zone).toInstant();
        Instant endAt = startAt.plus(Duration.ofMinutes(snapshot.durationMinutes()));
        if (hasSlotConflict(staff.getId(), patient.getId(), startAt, endAt)) {
            return;
        }

        Instant createdAt = ZonedDateTime.of(date.minusDays(2), LocalTime.of(10, 0), zone).toInstant();
        Instant approvedAt = ZonedDateTime.of(date.minusDays(1), LocalTime.of(11, 0), zone).toInstant();
        Instant completedAt = endAt.plusSeconds(300);
        if (completedAt.isAfter(now)) {
            completedAt = now;
        }

        Appointment appointment = Appointment.create(
                appointmentId, patient.getId(), staff.getId(), branch.getId(), service.getId(),
                TimeInterval.of(startAt, endAt), snapshot, createdAt
        );
        appointment.approve(staffUser.getId(), Clock.fixed(approvedAt, ZoneOffset.UTC));
        appointment.complete(staffUser.getId(), Clock.fixed(completedAt, ZoneOffset.UTC));

        persistAppointmentAndHistory(appointment, List.of(
                new AppointmentStatusHistoryJpaEntity(
                        DemoDataCatalog.id("history:" + historyKey + ":1"),
                        appointment.getId(), null, AppointmentStatus.PENDING,
                        patientUser.getId(), "Initial demo appointment booking", createdAt
                ),
                new AppointmentStatusHistoryJpaEntity(
                        DemoDataCatalog.id("history:" + historyKey + ":2"),
                        appointment.getId(), AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED,
                        staffUser.getId(), "Appointment approved", approvedAt
                ),
                new AppointmentStatusHistoryJpaEntity(
                        DemoDataCatalog.id("history:" + historyKey + ":3"),
                        appointment.getId(), AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED,
                        staffUser.getId(), "Appointment completed", completedAt
                )
        ));
    }

    private void seedConfirmedAppointment(UUID appointmentId, PatientProfile patient, UserAccount patientUser,
                                          StaffProfile staff, UserAccount staffUser, Branch branch, BranchService service,
                                          LocalDate date, LocalTime startTime, ZoneId zone, Instant now) {
        if (appointmentJpaRepository.existsById(appointmentId)) {
            return;
        }
        ServiceSnapshot snapshot = service.toSnapshot();
        Instant startAt = ZonedDateTime.of(date, startTime, zone).toInstant();
        Instant endAt = startAt.plus(Duration.ofMinutes(snapshot.durationMinutes()));
        if (hasSlotConflict(staff.getId(), patient.getId(), startAt, endAt)) {
            return;
        }

        Instant createdAt = minInstant(now.minusSeconds(3600), startAt.minusSeconds(7200));
        Instant approvedAt = minInstant(now.minusSeconds(1800), startAt.minusSeconds(3600));

        Appointment appointment = Appointment.create(
                appointmentId, patient.getId(), staff.getId(), branch.getId(), service.getId(),
                TimeInterval.of(startAt, endAt), snapshot, createdAt
        );
        appointment.approve(staffUser.getId(), Clock.fixed(approvedAt, ZoneOffset.UTC));

        persistAppointmentAndHistory(appointment, List.of(
                new AppointmentStatusHistoryJpaEntity(
                        DemoDataCatalog.id("history:confirmed-ahmet:1"),
                        appointment.getId(), null, AppointmentStatus.PENDING,
                        patientUser.getId(), "Initial demo appointment booking", createdAt
                ),
                new AppointmentStatusHistoryJpaEntity(
                        DemoDataCatalog.id("history:confirmed-ahmet:2"),
                        appointment.getId(), AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED,
                        staffUser.getId(), "Appointment approved", approvedAt
                )
        ));
    }

    private void seedPendingAppointment(UUID appointmentId, PatientProfile patient, UserAccount patientUser,
                                        StaffProfile staff, Branch branch, BranchService service,
                                        LocalDate date, LocalTime startTime, ZoneId zone, Instant now) {
        if (appointmentJpaRepository.existsById(appointmentId)) {
            return;
        }
        ServiceSnapshot snapshot = service.toSnapshot();
        Instant startAt = ZonedDateTime.of(date, startTime, zone).toInstant();
        Instant endAt = startAt.plus(Duration.ofMinutes(snapshot.durationMinutes()));
        if (hasSlotConflict(staff.getId(), patient.getId(), startAt, endAt)) {
            return;
        }

        Instant createdAt = minInstant(now.minusSeconds(2400), startAt.minusSeconds(7200));
        Appointment appointment = Appointment.create(
                appointmentId, patient.getId(), staff.getId(), branch.getId(), service.getId(),
                TimeInterval.of(startAt, endAt), snapshot, createdAt
        );

        persistAppointmentAndHistory(appointment, List.of(
                new AppointmentStatusHistoryJpaEntity(
                        DemoDataCatalog.id("history:" + appointmentId + ":1"),
                        appointment.getId(), null, AppointmentStatus.PENDING,
                        patientUser.getId(), "Initial demo appointment booking", createdAt
                )
        ));
    }

    private void seedRejectedAppointment(UUID appointmentId, PatientProfile patient, UserAccount patientUser,
                                         StaffProfile staff, UserAccount staffUser, Branch branch, BranchService service,
                                         LocalDate date, LocalTime startTime, ZoneId zone, Instant now) {
        if (appointmentJpaRepository.existsById(appointmentId)) {
            return;
        }
        ServiceSnapshot snapshot = service.toSnapshot();
        Instant startAt = ZonedDateTime.of(date, startTime, zone).toInstant();
        Instant endAt = startAt.plus(Duration.ofMinutes(snapshot.durationMinutes()));

        Instant createdAt = minInstant(now.minusSeconds(3000), startAt.minusSeconds(7200));
        Instant rejectedAt = minInstant(now.minusSeconds(1200), startAt.minusSeconds(3600));
        String reason = "Hekim klinik toplantısı nedeniyle uygun başka bir saat önerildi.";

        Appointment appointment = Appointment.create(
                appointmentId, patient.getId(), staff.getId(), branch.getId(), service.getId(),
                TimeInterval.of(startAt, endAt), snapshot, createdAt
        );
        appointment.reject(staffUser.getId(), reason, Clock.fixed(rejectedAt, ZoneOffset.UTC));

        persistAppointmentAndHistory(appointment, List.of(
                new AppointmentStatusHistoryJpaEntity(
                        DemoDataCatalog.id("history:rejected-ayse:1"),
                        appointment.getId(), null, AppointmentStatus.PENDING,
                        patientUser.getId(), "Initial demo appointment booking", createdAt
                ),
                new AppointmentStatusHistoryJpaEntity(
                        DemoDataCatalog.id("history:rejected-ayse:2"),
                        appointment.getId(), AppointmentStatus.PENDING, AppointmentStatus.REJECTED,
                        staffUser.getId(), reason, rejectedAt
                )
        ));
    }

    private void seedCancelledAppointment(UUID appointmentId, PatientProfile patient, UserAccount patientUser,
                                          StaffProfile staff, UserAccount staffUser, Branch branch, BranchService service,
                                          LocalDate date, LocalTime startTime, ZoneId zone, Instant now) {
        if (appointmentJpaRepository.existsById(appointmentId)) {
            return;
        }
        ServiceSnapshot snapshot = service.toSnapshot();
        Instant startAt = ZonedDateTime.of(date, startTime, zone).toInstant();
        Instant endAt = startAt.plus(Duration.ofMinutes(snapshot.durationMinutes()));

        Instant createdAt = minInstant(now.minusSeconds(3600), startAt.minusSeconds(10800));
        Instant approvedAt = minInstant(now.minusSeconds(2400), startAt.minusSeconds(7200));
        Instant cancelledAt = minInstant(now.minusSeconds(900), startAt.minusSeconds(3600));
        String reason = "Hasta program değişikliği nedeniyle randevusunu iptal etti.";

        Appointment appointment = Appointment.create(
                appointmentId, patient.getId(), staff.getId(), branch.getId(), service.getId(),
                TimeInterval.of(startAt, endAt), snapshot, createdAt
        );
        appointment.approve(staffUser.getId(), Clock.fixed(approvedAt, ZoneOffset.UTC));
        appointment.cancel(patientUser.getId(), reason, Clock.fixed(cancelledAt, ZoneOffset.UTC));

        persistAppointmentAndHistory(appointment, List.of(
                new AppointmentStatusHistoryJpaEntity(
                        DemoDataCatalog.id("history:cancelled-ahmet:1"),
                        appointment.getId(), null, AppointmentStatus.PENDING,
                        patientUser.getId(), "Initial demo appointment booking", createdAt
                ),
                new AppointmentStatusHistoryJpaEntity(
                        DemoDataCatalog.id("history:cancelled-ahmet:2"),
                        appointment.getId(), AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED,
                        staffUser.getId(), "Appointment approved", approvedAt
                ),
                new AppointmentStatusHistoryJpaEntity(
                        DemoDataCatalog.id("history:cancelled-ahmet:3"),
                        appointment.getId(), AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED,
                        patientUser.getId(), reason, cancelledAt
                )
        ));
    }

    private boolean hasSlotConflict(UUID staffId, UUID patientId, Instant startAt, Instant endAt) {
        return !appointmentJpaRepository.findOverlappingAppointmentsForStaff(
                staffId, startAt, endAt, BLOCKING_STATUSES
        ).isEmpty() || !appointmentJpaRepository.findOverlappingAppointmentsForPatient(
                patientId, startAt, endAt, BLOCKING_STATUSES
        ).isEmpty();
    }

    private void persistAppointmentAndHistory(Appointment appointment,
                                              List<AppointmentStatusHistoryJpaEntity> historyEntries) {
        appointmentJpaRepository.saveAndFlush(AppointmentJpaEntity.fromDomain(appointment, 0L));
        appointmentStatusHistoryJpaRepository.saveAllAndFlush(historyEntries);
    }

    private LocalDate resolvePreviousWorkday(LocalDate today) {
        LocalDate candidate = today.minusDays(1);
        while (candidate.getDayOfWeek() == DayOfWeek.SATURDAY || candidate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            candidate = candidate.minusDays(1);
        }
        return candidate;
    }

    private LocalDate resolveNextWorkday(LocalDate today) {
        LocalDate candidate = today.plusDays(1);
        while (candidate.getDayOfWeek() == DayOfWeek.SATURDAY || candidate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }

    private Instant minInstant(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }
}
