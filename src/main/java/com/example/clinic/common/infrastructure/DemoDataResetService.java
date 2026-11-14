package com.example.clinic.common.infrastructure;

import com.example.clinic.booking.infrastructure.persistence.AppointmentJpaRepository;
import com.example.clinic.booking.infrastructure.persistence.AppointmentStatusHistoryJpaRepository;
import com.example.clinic.catalog.infrastructure.BranchServiceRepository;
import com.example.clinic.catalog.infrastructure.ServiceDefinitionRepository;
import com.example.clinic.identity.infrastructure.MembershipRepository;
import com.example.clinic.identity.infrastructure.PatientProfileRepository;
import com.example.clinic.identity.infrastructure.StaffProfileRepository;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import com.example.clinic.organization.infrastructure.BranchOpeningHoursRepository;
import com.example.clinic.organization.infrastructure.BranchRepository;
import com.example.clinic.organization.infrastructure.DepartmentRepository;
import com.example.clinic.organization.infrastructure.OrganizationRepository;
import com.example.clinic.staff.domain.StaffAssignment;
import com.example.clinic.staff.infrastructure.AvailabilityExceptionRepository;
import com.example.clinic.staff.infrastructure.StaffAssignmentRepository;
import com.example.clinic.staff.infrastructure.StaffServiceEligibilityRepository;
import com.example.clinic.staff.infrastructure.WeeklyAvailabilityRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Controlled, FK-safe reset service that deletes only demonstration entities identified via
 * {@link DemoDataCatalog} while preserving unrelated records inside the same schema (FR-034).
 * Uses transactional JPA operations in strict reverse-dependency order without disabling constraints or truncating tables.
 */
@Service
@Profile("dev")
public class DemoDataResetService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataResetService.class);

    private final DemoEnvironmentGuard guard;
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
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;

    public DemoDataResetService(DemoEnvironmentGuard guard,
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
                                ApplicationEventPublisher eventPublisher,
                                EntityManager entityManager) {
        this.guard = Objects.requireNonNull(guard, "guard must not be null");
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
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager must not be null");
    }

    /**
     * Removes only demo records in strict reverse-FK dependency order inside a single transaction.
     *
     * @return total number of target entity identifiers scheduled across demo layers
     */
    @Transactional
    public int resetDemoData() {
        guard.assertSafeForReset();

        Set<UUID> orgIds = resolveDemoOrganizationIds();
        Set<UUID> branchIds = resolveDemoBranchIds(orgIds);
        Set<UUID> departmentIds = resolveDemoDepartmentIds(branchIds);
        Set<UUID> serviceDefIds = resolveDemoServiceDefinitionIds(orgIds);
        Set<UUID> branchServiceIds = resolveDemoBranchServiceIds(orgIds, branchIds);
        Set<UUID> userIds = resolveDemoUserIds();
        Set<UUID> staffIds = resolveDemoStaffIds(userIds);
        Set<UUID> patientIds = resolveDemoPatientIds(userIds);
        Set<UUID> assignmentIds = resolveDemoAssignmentIds(branchIds, staffIds);
        Set<UUID> appointmentIds = resolveDemoAppointmentIds(branchIds, branchServiceIds, patientIds, staffIds);
        Set<UUID> historyIds = resolveDemoHistoryIds(appointmentIds, userIds);
        Set<UUID> exceptionIds = resolveExceptionIds(assignmentIds);
        Set<UUID> weeklyAvailIds = resolveWeeklyAvailabilityIds(assignmentIds);
        Set<UUID> eligibilityIds = resolveEligibilityIds(assignmentIds, branchServiceIds);
        Set<UUID> openingHourIds = resolveOpeningHourIds(branchIds);
        Set<UUID> membershipIds = resolveMembershipIds(orgIds, userIds);

        entityManager.flush();
        entityManager.clear();

        int totalDeletedTargetGroups = 0;
        totalDeletedTargetGroups += deleteBatch(appointmentStatusHistoryJpaRepository, historyIds);
        totalDeletedTargetGroups += deleteBatch(appointmentJpaRepository, appointmentIds);
        totalDeletedTargetGroups += deleteBatch(availabilityExceptionRepository, exceptionIds);
        totalDeletedTargetGroups += deleteBatch(weeklyAvailabilityRepository, weeklyAvailIds);
        totalDeletedTargetGroups += deleteBatch(staffServiceEligibilityRepository, eligibilityIds);
        totalDeletedTargetGroups += deleteBatch(staffAssignmentRepository, assignmentIds);
        totalDeletedTargetGroups += deleteBatch(branchServiceRepository, branchServiceIds);
        totalDeletedTargetGroups += deleteBatch(serviceDefinitionRepository, serviceDefIds);
        totalDeletedTargetGroups += deleteBatch(departmentRepository, departmentIds);
        totalDeletedTargetGroups += deleteBatch(branchOpeningHoursRepository, openingHourIds);
        totalDeletedTargetGroups += deleteBatch(membershipRepository, membershipIds);
        totalDeletedTargetGroups += deleteBatch(patientProfileRepository, patientIds);
        totalDeletedTargetGroups += deleteBatch(staffProfileRepository, staffIds);
        totalDeletedTargetGroups += deleteBatch(userAccountRepository, userIds);
        totalDeletedTargetGroups += deleteBatch(branchRepository, branchIds);
        totalDeletedTargetGroups += deleteBatch(organizationRepository, orgIds);

        entityManager.flush();
        entityManager.clear();

        eventPublisher.publishEvent(new DemoDataResetCompletedEvent(totalDeletedTargetGroups));
        log.info("Scoped demo data reset completed safely ({} scoped target IDs processed).", totalDeletedTargetGroups);
        return totalDeletedTargetGroups;
    }

    private <T> int deleteBatch(org.springframework.data.jpa.repository.JpaRepository<T, UUID> repository, Set<UUID> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        repository.deleteAllByIdInBatch(ids);
        return ids.size();
    }

    private Set<UUID> resolveDemoOrganizationIds() {
        Set<UUID> ids = new LinkedHashSet<>();
        if (organizationRepository.existsById(DemoDataCatalog.ORG_ID)) {
            ids.add(DemoDataCatalog.ORG_ID);
        }
        organizationRepository.findByName(DemoDataCatalog.ORG_NAME)
                .ifPresent(org -> ids.add(org.getId()));
        return ids;
    }

    private Set<UUID> resolveDemoBranchIds(Set<UUID> orgIds) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (UUID branchId : DemoDataCatalog.ALL_BRANCH_IDS) {
            if (branchRepository.existsById(branchId)) {
                ids.add(branchId);
            }
        }
        for (UUID orgId : orgIds) {
            branchRepository.findByOrganizationId(orgId).forEach(b -> ids.add(b.getId()));
        }
        return ids;
    }

    private Set<UUID> resolveDemoDepartmentIds(Set<UUID> branchIds) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (UUID deptId : DemoDataCatalog.ALL_DEPARTMENT_IDS) {
            if (departmentRepository.existsById(deptId)) {
                ids.add(deptId);
            }
        }
        for (UUID branchId : branchIds) {
            departmentRepository.findByBranchId(branchId).forEach(d -> ids.add(d.getId()));
        }
        return ids;
    }

    private Set<UUID> resolveDemoServiceDefinitionIds(Set<UUID> orgIds) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (UUID defId : DemoDataCatalog.ALL_SERVICE_DEFINITION_IDS) {
            if (serviceDefinitionRepository.existsById(defId)) {
                ids.add(defId);
            }
        }
        for (UUID orgId : orgIds) {
            serviceDefinitionRepository.findByOrganizationId(orgId).forEach(sd -> ids.add(sd.getId()));
        }
        return ids;
    }

    private Set<UUID> resolveDemoBranchServiceIds(Set<UUID> orgIds, Set<UUID> branchIds) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (UUID bsId : DemoDataCatalog.ALL_BRANCH_SERVICE_IDS) {
            if (branchServiceRepository.existsById(bsId)) {
                ids.add(bsId);
            }
        }
        for (UUID branchId : branchIds) {
            branchServiceRepository.findByBranchId(branchId).forEach(bs -> ids.add(bs.getId()));
        }
        for (UUID orgId : orgIds) {
            branchServiceRepository.findByOrganizationId(orgId).forEach(bs -> ids.add(bs.getId()));
        }
        return ids;
    }

    private Set<UUID> resolveDemoUserIds() {
        Set<UUID> ids = new LinkedHashSet<>();
        for (UUID userId : DemoDataCatalog.ALL_USER_IDS) {
            if (userAccountRepository.existsById(userId)) {
                ids.add(userId);
            }
        }
        for (String email : DemoDataCatalog.ALL_ACCOUNT_EMAILS) {
            userAccountRepository.findByEmail(email).ifPresent(user -> ids.add(user.getId()));
        }
        return ids;
    }

    private Set<UUID> resolveDemoStaffIds(Set<UUID> userIds) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (UUID staffId : DemoDataCatalog.ALL_STAFF_PROFILE_IDS) {
            if (staffProfileRepository.existsById(staffId)) {
                ids.add(staffId);
            }
        }
        for (UUID userId : userIds) {
            staffProfileRepository.findByUserAccountId(userId).ifPresent(sp -> ids.add(sp.getId()));
        }
        return ids;
    }

    private Set<UUID> resolveDemoPatientIds(Set<UUID> userIds) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (UUID patientId : DemoDataCatalog.ALL_PATIENT_PROFILE_IDS) {
            if (patientProfileRepository.existsById(patientId)) {
                ids.add(patientId);
            }
        }
        for (UUID userId : userIds) {
            patientProfileRepository.findByUserAccountId(userId).ifPresent(pp -> ids.add(pp.getId()));
        }
        return ids;
    }

    private Set<UUID> resolveDemoAssignmentIds(Set<UUID> branchIds, Set<UUID> staffIds) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (UUID assignmentId : DemoDataCatalog.ALL_ASSIGNMENT_IDS) {
            if (staffAssignmentRepository.existsById(assignmentId)) {
                ids.add(assignmentId);
            }
        }
        for (UUID branchId : branchIds) {
            staffAssignmentRepository.findByBranchId(branchId).stream()
                    .map(StaffAssignment::getId)
                    .forEach(ids::add);
        }
        for (UUID staffId : staffIds) {
            staffAssignmentRepository.findByStaffId(staffId).stream()
                    .map(StaffAssignment::getId)
                    .forEach(ids::add);
        }
        return ids;
    }

    private Set<UUID> resolveDemoAppointmentIds(Set<UUID> branchIds, Set<UUID> branchServiceIds,
                                                Set<UUID> patientIds, Set<UUID> staffIds) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (UUID apptId : DemoDataCatalog.ALL_APPOINTMENT_IDS) {
            if (appointmentJpaRepository.existsById(apptId)) {
                ids.add(apptId);
            }
        }
        if (!branchIds.isEmpty()) {
            ids.addAll(appointmentJpaRepository.findIdsByBranchIdIn(branchIds));
        }
        if (!branchServiceIds.isEmpty()) {
            ids.addAll(appointmentJpaRepository.findIdsByBranchServiceIdIn(branchServiceIds));
        }
        if (!patientIds.isEmpty()) {
            ids.addAll(appointmentJpaRepository.findIdsByPatientIdIn(patientIds));
        }
        if (!staffIds.isEmpty()) {
            ids.addAll(appointmentJpaRepository.findIdsByStaffIdIn(staffIds));
        }
        return ids;
    }

    private Set<UUID> resolveDemoHistoryIds(Set<UUID> appointmentIds, Set<UUID> userIds) {
        Set<UUID> ids = new LinkedHashSet<>();
        if (!appointmentIds.isEmpty()) {
            ids.addAll(appointmentStatusHistoryJpaRepository.findIdsByAppointmentIdIn(appointmentIds));
        }
        if (!userIds.isEmpty()) {
            ids.addAll(appointmentStatusHistoryJpaRepository.findIdsByActorUserIdIn(userIds));
        }
        return ids;
    }

    private Set<UUID> resolveExceptionIds(Set<UUID> assignmentIds) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (UUID assignmentId : assignmentIds) {
            availabilityExceptionRepository.findByAssignmentId(assignmentId)
                    .forEach(ex -> ids.add(ex.getId()));
        }
        return ids;
    }

    private Set<UUID> resolveWeeklyAvailabilityIds(Set<UUID> assignmentIds) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (UUID assignmentId : assignmentIds) {
            weeklyAvailabilityRepository.findByAssignmentId(assignmentId)
                    .forEach(w -> ids.add(w.getId()));
        }
        return ids;
    }

    private Set<UUID> resolveEligibilityIds(Set<UUID> assignmentIds, Set<UUID> branchServiceIds) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (UUID assignmentId : assignmentIds) {
            staffServiceEligibilityRepository.findByAssignmentId(assignmentId)
                    .forEach(e -> ids.add(e.getId()));
        }
        for (UUID branchServiceId : branchServiceIds) {
            staffServiceEligibilityRepository.findByBranchServiceId(branchServiceId)
                    .forEach(e -> ids.add(e.getId()));
        }
        return ids;
    }

    private Set<UUID> resolveOpeningHourIds(Set<UUID> branchIds) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (UUID branchId : branchIds) {
            branchOpeningHoursRepository.findByBranchId(branchId)
                    .forEach(oh -> ids.add(oh.getId()));
        }
        return ids;
    }

    private Set<UUID> resolveMembershipIds(Set<UUID> orgIds, Set<UUID> userIds) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (UUID orgId : orgIds) {
            membershipRepository.findByOrganizationId(orgId)
                    .forEach(m -> ids.add(m.getId()));
        }
        for (UUID userId : userIds) {
            membershipRepository.findByUserAccountId(userId)
                    .forEach(m -> ids.add(m.getId()));
        }
        return ids;
    }
}
