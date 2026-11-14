package com.example.clinic.staff.application;

import com.example.clinic.catalog.domain.BranchService;
import com.example.clinic.catalog.infrastructure.BranchServiceRepository;
import com.example.clinic.common.security.SecurityContextFacade;
import com.example.clinic.identity.application.AccessPolicy;
import com.example.clinic.identity.application.AccountCredentialService;
import com.example.clinic.identity.domain.Membership;
import com.example.clinic.identity.domain.Role;
import com.example.clinic.identity.domain.StaffProfile;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.MembershipRepository;
import com.example.clinic.identity.infrastructure.StaffProfileRepository;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.Department;
import com.example.clinic.organization.domain.Organization;
import com.example.clinic.organization.infrastructure.BranchRepository;
import com.example.clinic.organization.infrastructure.DepartmentRepository;
import com.example.clinic.staff.application.dto.AssignStaffCommand;
import com.example.clinic.staff.application.dto.AvailabilityExceptionResponse;
import com.example.clinic.staff.application.dto.ConfigureEligibilityCommand;
import com.example.clinic.staff.application.dto.ConfigureWeeklyAvailabilityCommand;
import com.example.clinic.staff.application.dto.CreateAvailabilityExceptionCommand;
import com.example.clinic.staff.application.dto.CreateStaffCommand;
import com.example.clinic.staff.application.dto.StaffAssignmentResponse;
import com.example.clinic.staff.application.dto.StaffProfileResponse;
import com.example.clinic.staff.application.dto.StaffServiceEligibilityResponse;
import com.example.clinic.staff.application.dto.WeeklyAvailabilityItem;
import com.example.clinic.staff.application.dto.WeeklyAvailabilityResponse;
import com.example.clinic.staff.domain.AvailabilityException;
import com.example.clinic.staff.domain.AvailabilityExceptionKind;
import com.example.clinic.staff.domain.StaffAssignment;
import com.example.clinic.staff.domain.StaffServiceEligibility;
import com.example.clinic.staff.domain.WeeklyAvailability;
import com.example.clinic.staff.infrastructure.AvailabilityExceptionRepository;
import com.example.clinic.staff.infrastructure.StaffAssignmentRepository;
import com.example.clinic.staff.infrastructure.StaffServiceEligibilityRepository;
import com.example.clinic.staff.infrastructure.WeeklyAvailabilityRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service orchestrating medical staff registration, branch assignments,
 * service eligibilities, recurring weekly availability, and exceptions.
 * Enforces multi-tenant authorization and domain integrity (FR-007..FR-011, TC-007..TC-011).
 */
@Service
@Transactional(readOnly = true)
public class StaffManagementService {

    private final StaffProfileRepository staffProfileRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final StaffServiceEligibilityRepository staffServiceEligibilityRepository;
    private final WeeklyAvailabilityRepository weeklyAvailabilityRepository;
    private final AvailabilityExceptionRepository availabilityExceptionRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final BranchServiceRepository branchServiceRepository;
    private final UserAccountRepository userAccountRepository;
    private final MembershipRepository membershipRepository;
    private final AccountCredentialService accountCredentialService;
    private final AccessPolicy accessPolicy;
    private final SecurityContextFacade securityContextFacade;

    public StaffManagementService(StaffProfileRepository staffProfileRepository,
                                  StaffAssignmentRepository staffAssignmentRepository,
                                  StaffServiceEligibilityRepository staffServiceEligibilityRepository,
                                  WeeklyAvailabilityRepository weeklyAvailabilityRepository,
                                  AvailabilityExceptionRepository availabilityExceptionRepository,
                                  BranchRepository branchRepository,
                                  DepartmentRepository departmentRepository,
                                  BranchServiceRepository branchServiceRepository,
                                  UserAccountRepository userAccountRepository,
                                  MembershipRepository membershipRepository,
                                  AccountCredentialService accountCredentialService,
                                  AccessPolicy accessPolicy,
                                  SecurityContextFacade securityContextFacade) {
        this.staffProfileRepository = staffProfileRepository;
        this.staffAssignmentRepository = staffAssignmentRepository;
        this.staffServiceEligibilityRepository = staffServiceEligibilityRepository;
        this.weeklyAvailabilityRepository = weeklyAvailabilityRepository;
        this.availabilityExceptionRepository = availabilityExceptionRepository;
        this.branchRepository = branchRepository;
        this.departmentRepository = departmentRepository;
        this.branchServiceRepository = branchServiceRepository;
        this.userAccountRepository = userAccountRepository;
        this.membershipRepository = membershipRepository;
        this.accountCredentialService = accountCredentialService;
        this.accessPolicy = accessPolicy;
        this.securityContextFacade = securityContextFacade;
    }

    // =========================================================================
    // 1. Staff Registration (FR-007, FR-008, TC-007, TC-008)
    // =========================================================================

    @Transactional
    public StaffProfileResponse registerStaff(CreateStaffCommand command) {
        Objects.requireNonNull(command, "CreateStaffCommand must not be null");
        UserAccount currentUser = securityContextFacade.getRequiredCurrentUser();
        return registerStaff(command, currentUser);
    }

    @Transactional
    public StaffProfileResponse registerStaff(CreateStaffCommand command, UserAccount actor) {
        Objects.requireNonNull(command, "CreateStaffCommand must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        Branch branch = branchRepository.findById(command.branchId())
                .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + command.branchId()));

        accessPolicy.enforceAdminBranchAccess(branch.getId(), actor);

        UserAccount userAccount = userAccountRepository.findByEmail(command.email().trim().toLowerCase(java.util.Locale.ROOT))
                .orElseGet(() -> accountCredentialService.createAccount(command.email(), command.password()));

        StaffProfile profile = new StaffProfile(
                userAccount,
                command.fullName(),
                command.phone(),
                command.staffType(),
                command.specialty()
        );
        StaffProfile savedProfile = staffProfileRepository.save(profile);

        boolean membershipExists = membershipRepository
                .findByUserAccountIdAndRole(userAccount.getId(), Role.ROLE_STAFF).stream()
                .anyMatch(m -> m.getBranch() != null && m.getBranch().getId().equals(branch.getId()));

        if (!membershipExists) {
            membershipRepository.save(new Membership(userAccount, branch.getOrganization(), branch, Role.ROLE_STAFF));
        }

        return mapToProfileResponse(savedProfile);
    }

    // =========================================================================
    // 2. Staff Branch Assignment (FR-007, STA-02, TC-007)
    // =========================================================================

    @Transactional
    public StaffAssignmentResponse assignStaffToBranch(AssignStaffCommand command) {
        Objects.requireNonNull(command, "AssignStaffCommand must not be null");
        UserAccount currentUser = securityContextFacade.getRequiredCurrentUser();
        return assignStaffToBranch(command, currentUser);
    }

    @Transactional
    public StaffAssignmentResponse assignStaffToBranch(AssignStaffCommand command, UserAccount actor) {
        Objects.requireNonNull(command, "AssignStaffCommand must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        accessPolicy.enforceAdminBranchAccess(command.branchId(), actor);

        StaffProfile staff = staffProfileRepository.findById(command.staffId())
                .orElseThrow(() -> new IllegalArgumentException("StaffProfile not found: " + command.staffId()));

        if (!staff.isActive()) {
            throw new IllegalArgumentException("Cannot assign inactive staff profile: " + staff.getId());
        }

        Branch branch = branchRepository.findById(command.branchId())
                .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + command.branchId()));

        if (!branch.isActive()) {
            throw new IllegalArgumentException("Cannot assign staff to inactive branch: " + branch.getId());
        }

        Department department = departmentRepository.findById(command.departmentId())
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + command.departmentId()));

        if (!department.isActive()) {
            throw new IllegalArgumentException("Cannot assign staff to inactive department: " + department.getId());
        }

        StaffAssignment.validateDepartmentBelongsToBranch(branch, department);

        if (staffAssignmentRepository.existsByStaffIdAndBranchIdAndDepartmentId(
                staff.getId(), branch.getId(), department.getId())) {
            throw new IllegalArgumentException("Staff is already assigned to this department at branch " + branch.getName());
        }

        StaffAssignment assignment = new StaffAssignment(staff, branch, department);
        StaffAssignment savedAssignment = staffAssignmentRepository.save(assignment);

        boolean membershipExists = membershipRepository
                .findByUserAccountIdAndRole(staff.getUserAccount().getId(), Role.ROLE_STAFF).stream()
                .anyMatch(m -> m.getBranch() != null && m.getBranch().getId().equals(branch.getId()));

        if (!membershipExists) {
            membershipRepository.save(new Membership(staff.getUserAccount(), branch.getOrganization(), branch, Role.ROLE_STAFF));
        }

        return mapToAssignmentResponse(savedAssignment);
    }

    // =========================================================================
    // 3. Service Eligibility (FR-009, STA-02, TC-009)
    // =========================================================================

    @Transactional
    public List<StaffServiceEligibilityResponse> configureEligibility(ConfigureEligibilityCommand command) {
        Objects.requireNonNull(command, "ConfigureEligibilityCommand must not be null");
        UserAccount currentUser = securityContextFacade.getRequiredCurrentUser();
        return configureEligibility(command, currentUser);
    }

    @Transactional
    public List<StaffServiceEligibilityResponse> configureEligibility(ConfigureEligibilityCommand command, UserAccount actor) {
        Objects.requireNonNull(command, "ConfigureEligibilityCommand must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        StaffAssignment assignment = staffAssignmentRepository.findById(command.assignmentId())
                .orElseThrow(() -> new IllegalArgumentException("StaffAssignment not found: " + command.assignmentId()));

        accessPolicy.enforceAdminBranchAccess(assignment.getBranch().getId(), actor);

        List<UUID> serviceIds = command.branchServiceIds() != null ? command.branchServiceIds() : Collections.emptyList();
        List<BranchService> validServices = new ArrayList<>();
        for (UUID serviceId : serviceIds) {
            BranchService service = branchServiceRepository.findById(serviceId)
                    .orElseThrow(() -> new IllegalArgumentException("BranchService not found: " + serviceId));
            StaffServiceEligibility.validateServiceBelongsToAssignmentBranch(assignment, service);
            validServices.add(service);
        }

        staffServiceEligibilityRepository.deleteByAssignmentId(assignment.getId());
        staffServiceEligibilityRepository.flush();

        List<StaffServiceEligibilityResponse> responses = new ArrayList<>();
        for (BranchService service : validServices) {
            StaffServiceEligibility saved = staffServiceEligibilityRepository.save(
                    new StaffServiceEligibility(assignment, service)
            );
            responses.add(mapToEligibilityResponse(saved));
        }

        return responses;
    }

    // =========================================================================
    // 4. Weekly Availability (FR-010, SCH-02, TC-010)
    // =========================================================================

    @Transactional
    public List<WeeklyAvailabilityResponse> configureWeeklyAvailability(ConfigureWeeklyAvailabilityCommand command) {
        Objects.requireNonNull(command, "ConfigureWeeklyAvailabilityCommand must not be null");
        UserAccount currentUser = securityContextFacade.getRequiredCurrentUser();
        return configureWeeklyAvailability(command, currentUser);
    }

    @Transactional
    public List<WeeklyAvailabilityResponse> configureWeeklyAvailability(ConfigureWeeklyAvailabilityCommand command, UserAccount actor) {
        Objects.requireNonNull(command, "ConfigureWeeklyAvailabilityCommand must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        StaffAssignment assignment = staffAssignmentRepository.findById(command.assignmentId())
                .orElseThrow(() -> new IllegalArgumentException("StaffAssignment not found: " + command.assignmentId()));

        enforceAdminOrStaffSelfAccess(assignment, actor);

        List<WeeklyAvailabilityItem> items = command.items() != null ? command.items() : Collections.emptyList();
        for (WeeklyAvailabilityItem item : items) {
            WeeklyAvailability.validateWeekday(item.weekday());
            WeeklyAvailability.validateTimeOrder(item.startTime(), item.endTime());
        }

        // Validate no internal overlaps for same weekday
        for (int i = 0; i < items.size(); i++) {
            WeeklyAvailabilityItem a = items.get(i);
            for (int j = i + 1; j < items.size(); j++) {
                WeeklyAvailabilityItem b = items.get(j);
                if (a.weekday() == b.weekday()) {
                    boolean overlap = a.startTime().isBefore(b.endTime()) && b.startTime().isBefore(a.endTime());
                    if (overlap) {
                        throw new IllegalArgumentException("Overlapping weekly availability windows on weekday " + a.weekday());
                    }
                }
            }
        }

        weeklyAvailabilityRepository.deleteByAssignmentId(assignment.getId());
        weeklyAvailabilityRepository.flush();

        List<WeeklyAvailabilityResponse> responses = new ArrayList<>();
        for (WeeklyAvailabilityItem item : items) {
            WeeklyAvailability saved = weeklyAvailabilityRepository.save(
                    new WeeklyAvailability(assignment, item.weekday(), item.startTime(), item.endTime())
            );
            responses.add(mapToWeeklyResponse(saved));
        }

        return responses;
    }

    // =========================================================================
    // 5. Availability Exceptions (FR-011, SCH-03, TC-011)
    // =========================================================================

    @Transactional
    public AvailabilityExceptionResponse addAvailabilityException(CreateAvailabilityExceptionCommand command) {
        Objects.requireNonNull(command, "CreateAvailabilityExceptionCommand must not be null");
        UserAccount currentUser = securityContextFacade.getRequiredCurrentUser();
        return addAvailabilityException(command, currentUser);
    }

    @Transactional
    public AvailabilityExceptionResponse addAvailabilityException(CreateAvailabilityExceptionCommand command, UserAccount actor) {
        Objects.requireNonNull(command, "CreateAvailabilityExceptionCommand must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        StaffAssignment assignment = staffAssignmentRepository.findById(command.assignmentId())
                .orElseThrow(() -> new IllegalArgumentException("StaffAssignment not found: " + command.assignmentId()));

        enforceAdminOrStaffSelfAccess(assignment, actor);

        AvailabilityException exception = new AvailabilityException(
                assignment,
                command.localDate(),
                command.kind(),
                command.startTime(),
                command.endTime(),
                command.reason()
        );
        AvailabilityException saved = availabilityExceptionRepository.save(exception);
        return mapToExceptionResponse(saved);
    }

    @Transactional
    public void deleteWeeklyAvailability(UUID assignmentId, UUID availabilityId) {
        Objects.requireNonNull(assignmentId, "assignmentId must not be null");
        Objects.requireNonNull(availabilityId, "availabilityId must not be null");

        StaffAssignment assignment = staffAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("StaffAssignment not found: " + assignmentId));
        enforceAdminOrStaffSelfAccess(assignment, securityContextFacade.getRequiredCurrentUser());

        WeeklyAvailability availability = weeklyAvailabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new IllegalArgumentException("WeeklyAvailability not found: " + availabilityId));

        if (!availability.getAssignment().getId().equals(assignmentId)) {
            throw new AccessDeniedException("Access denied: availability record does not belong to assignment " + assignmentId);
        }

        weeklyAvailabilityRepository.delete(availability);
    }

    @Transactional
    public void deleteAvailabilityException(UUID assignmentId, UUID exceptionId) {
        Objects.requireNonNull(assignmentId, "assignmentId must not be null");
        Objects.requireNonNull(exceptionId, "exceptionId must not be null");

        StaffAssignment assignment = staffAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("StaffAssignment not found: " + assignmentId));
        enforceAdminOrStaffSelfAccess(assignment, securityContextFacade.getRequiredCurrentUser());

        AvailabilityException exception = availabilityExceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new IllegalArgumentException("AvailabilityException not found: " + exceptionId));

        if (!exception.getAssignment().getId().equals(assignmentId)) {
            throw new AccessDeniedException("Access denied: exception record does not belong to assignment " + assignmentId);
        }

        availabilityExceptionRepository.delete(exception);
    }

    // =========================================================================
    // 6. Query Methods
    // =========================================================================

    public StaffProfileResponse getAuthorizedStaffProfile(UUID staffId) {
        Objects.requireNonNull(staffId, "staffId must not be null");
        UserAccount currentAdmin = securityContextFacade.getRequiredCurrentUser();
        StaffProfile profile = staffProfileRepository.findById(staffId)
                .orElseThrow(() -> new AccessDeniedException("Access denied: staff profile not accessible: " + staffId));

        enforceAdminStaffAccess(profile, currentAdmin);
        return mapToProfileResponse(profile);
    }

    public List<StaffProfileResponse> getAuthorizedStaffProfiles() {
        UserAccount currentAdmin = securityContextFacade.getRequiredCurrentUser();
        List<UUID> authorizedBranchIds = accessPolicy.getAdminAuthorizedBranchIds(currentAdmin);
        if (authorizedBranchIds.isEmpty()) {
            return Collections.emptyList();
        }
        return staffProfileRepository.findAuthorizedStaffByBranchIds(authorizedBranchIds).stream()
                .map(this::mapToProfileResponse)
                .toList();
    }

    public List<StaffAssignmentResponse> getAuthorizedStaffAssignments() {
        UserAccount currentAdmin = securityContextFacade.getRequiredCurrentUser();
        List<UUID> authorizedBranchIds = accessPolicy.getAdminAuthorizedBranchIds(currentAdmin);
        if (authorizedBranchIds.isEmpty()) {
            return Collections.emptyList();
        }
        return staffAssignmentRepository.findByBranchIdInOrderByCreatedAtAsc(authorizedBranchIds).stream()
                .map(this::mapToAssignmentResponse)
                .toList();
    }

    public List<StaffAssignmentResponse> getAuthorizedStaffAssignmentsByStaff(UUID staffId) {
        Objects.requireNonNull(staffId, "staffId must not be null");
        UserAccount currentAdmin = securityContextFacade.getRequiredCurrentUser();
        StaffProfile profile = staffProfileRepository.findById(staffId)
                .orElseThrow(() -> new AccessDeniedException("Access denied: staff profile not accessible: " + staffId));

        enforceAdminStaffAccess(profile, currentAdmin);
        List<UUID> authorizedBranchIds = accessPolicy.getAdminAuthorizedBranchIds(currentAdmin);

        return staffAssignmentRepository.findByStaffId(staffId).stream()
                .filter(a -> authorizedBranchIds.contains(a.getBranch().getId()))
                .map(this::mapToAssignmentResponse)
                .toList();
    }

    public List<StaffServiceEligibilityResponse> getAuthorizedEligibilitiesByAssignment(UUID assignmentId) {
        Objects.requireNonNull(assignmentId, "assignmentId must not be null");
        StaffAssignment assignment = staffAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new AccessDeniedException("Access denied: assignment not accessible: " + assignmentId));
        enforceAdminOrStaffSelfAccess(assignment, securityContextFacade.getRequiredCurrentUser());
        return staffServiceEligibilityRepository.findByAssignmentId(assignmentId).stream()
                .map(this::mapToEligibilityResponse)
                .toList();
    }

    public List<WeeklyAvailabilityResponse> getAuthorizedWeeklyAvailabilitiesByAssignment(UUID assignmentId) {
        Objects.requireNonNull(assignmentId, "assignmentId must not be null");
        StaffAssignment assignment = staffAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new AccessDeniedException("Access denied: assignment not accessible: " + assignmentId));
        enforceAdminOrStaffSelfAccess(assignment, securityContextFacade.getRequiredCurrentUser());
        return weeklyAvailabilityRepository.findByAssignmentId(assignmentId).stream()
                .sorted(Comparator.comparing(WeeklyAvailability::getWeekday).thenComparing(WeeklyAvailability::getStartTime))
                .map(this::mapToWeeklyResponse)
                .toList();
    }

    public List<AvailabilityExceptionResponse> getAuthorizedAvailabilityExceptionsByAssignment(UUID assignmentId) {
        Objects.requireNonNull(assignmentId, "assignmentId must not be null");
        StaffAssignment assignment = staffAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new AccessDeniedException("Access denied: assignment not accessible: " + assignmentId));
        enforceAdminOrStaffSelfAccess(assignment, securityContextFacade.getRequiredCurrentUser());
        return availabilityExceptionRepository.findByAssignmentId(assignmentId).stream()
                .sorted(Comparator.comparing(AvailabilityException::getLocalDate))
                .map(this::mapToExceptionResponse)
                .toList();
    }

    public StaffProfileResponse getStaffProfile(UUID staffId) {
        StaffProfile profile = staffProfileRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("StaffProfile not found: " + staffId));
        return mapToProfileResponse(profile);
    }

    public List<StaffProfileResponse> getAllStaffProfiles() {
        return staffProfileRepository.findAll().stream()
                .map(this::mapToProfileResponse)
                .toList();
    }

    public List<StaffAssignmentResponse> getAllStaffAssignments() {
        return staffAssignmentRepository.findAll().stream()
                .map(this::mapToAssignmentResponse)
                .toList();
    }

    public List<StaffAssignmentResponse> getStaffAssignmentsByBranch(UUID branchId) {
        return staffAssignmentRepository.findByBranchId(branchId).stream()
                .map(this::mapToAssignmentResponse)
                .toList();
    }

    public List<StaffAssignmentResponse> getStaffAssignmentsByStaff(UUID staffId) {
        return staffAssignmentRepository.findByStaffId(staffId).stream()
                .map(this::mapToAssignmentResponse)
                .toList();
    }

    public List<StaffServiceEligibilityResponse> getEligibilitiesByAssignment(UUID assignmentId) {
        return staffServiceEligibilityRepository.findByAssignmentId(assignmentId).stream()
                .map(this::mapToEligibilityResponse)
                .toList();
    }

    public List<WeeklyAvailabilityResponse> getWeeklyAvailabilitiesByAssignment(UUID assignmentId) {
        return weeklyAvailabilityRepository.findByAssignmentId(assignmentId).stream()
                .sorted(Comparator.comparing(WeeklyAvailability::getWeekday).thenComparing(WeeklyAvailability::getStartTime))
                .map(this::mapToWeeklyResponse)
                .toList();
    }

    public List<AvailabilityExceptionResponse> getAvailabilityExceptionsByAssignment(UUID assignmentId) {
        return availabilityExceptionRepository.findByAssignmentId(assignmentId).stream()
                .sorted(Comparator.comparing(AvailabilityException::getLocalDate))
                .map(this::mapToExceptionResponse)
                .toList();
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    private void enforceAdminStaffAccess(StaffProfile profile, UserAccount actor) {
        List<UUID> authorizedBranchIds = accessPolicy.getAdminAuthorizedBranchIds(actor);
        if (authorizedBranchIds.isEmpty()) {
            throw new AccessDeniedException("Access denied: administrator has no authorized branches.");
        }

        boolean assignedInAuthorizedBranch = staffAssignmentRepository.findByStaffId(profile.getId()).stream()
                .anyMatch(a -> authorizedBranchIds.contains(a.getBranch().getId()));
        if (assignedInAuthorizedBranch) {
            return;
        }

        boolean membershipInAuthorizedBranch = membershipRepository
                .findByUserAccountIdAndRole(profile.getUserAccount().getId(), Role.ROLE_STAFF).stream()
                .anyMatch(m -> m.getBranch() != null && authorizedBranchIds.contains(m.getBranch().getId()));
        if (membershipInAuthorizedBranch) {
            return;
        }

        throw new AccessDeniedException("Access denied: administrator is not authorized for staff " + profile.getId());
    }

    private void enforceAdminOrStaffSelfAccess(StaffAssignment assignment, UserAccount actor) {
        boolean isStaffOwner = assignment.getStaff().getUserAccount().getId().equals(actor.getId());
        if (isStaffOwner) {
            return;
        }

        try {
            accessPolicy.enforceAdminBranchAccess(assignment.getBranch().getId(), actor);
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException("Access denied: actor is neither the assigned staff member nor branch administrator");
        }
    }

    private StaffProfileResponse mapToProfileResponse(StaffProfile profile) {
        return new StaffProfileResponse(
                profile.getId(),
                profile.getUserAccount().getId(),
                profile.getUserAccount().getEmail(),
                profile.getFullName(),
                profile.getPhone(),
                profile.getStaffType(),
                profile.getSpecialty(),
                profile.isActive(),
                profile.getCreatedAt()
        );
    }

    private StaffAssignmentResponse mapToAssignmentResponse(StaffAssignment assignment) {
        return new StaffAssignmentResponse(
                assignment.getId(),
                assignment.getStaff().getId(),
                assignment.getStaff().getFullName(),
                assignment.getStaff().getStaffType(),
                assignment.getStaff().getSpecialty(),
                assignment.getBranch().getId(),
                assignment.getBranch().getName(),
                assignment.getDepartment().getId(),
                assignment.getDepartment().getName(),
                assignment.isActive(),
                assignment.getCreatedAt()
        );
    }

    private StaffServiceEligibilityResponse mapToEligibilityResponse(StaffServiceEligibility eligibility) {
        BranchService service = eligibility.getBranchService();
        return new StaffServiceEligibilityResponse(
                eligibility.getId(),
                eligibility.getAssignment().getId(),
                service.getId(),
                service.getServiceDefinition().getName(),
                service.getDurationMinutes(),
                service.getFeeAmount(),
                service.getCurrency()
        );
    }

    private WeeklyAvailabilityResponse mapToWeeklyResponse(WeeklyAvailability weekly) {
        return new WeeklyAvailabilityResponse(
                weekly.getId(),
                weekly.getAssignment().getId(),
                weekly.getWeekday(),
                weekly.getStartTime(),
                weekly.getEndTime()
        );
    }

    private AvailabilityExceptionResponse mapToExceptionResponse(AvailabilityException exception) {
        return new AvailabilityExceptionResponse(
                exception.getId(),
                exception.getAssignment().getId(),
                exception.getLocalDate(),
                exception.getKind(),
                exception.getStartTime(),
                exception.getEndTime(),
                exception.getReason(),
                exception.getCreatedAt()
        );
    }
}
