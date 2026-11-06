package com.example.clinic.organization.application;

import com.example.clinic.common.security.SecurityContextFacade;
import com.example.clinic.identity.application.AccessPolicy;
import com.example.clinic.identity.domain.Membership;
import com.example.clinic.identity.domain.Role;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.MembershipRepository;
import com.example.clinic.organization.application.dto.BranchResponse;
import com.example.clinic.organization.application.dto.CreateBranchCommand;
import com.example.clinic.organization.application.dto.CreateDepartmentCommand;
import com.example.clinic.organization.application.dto.CreateOrganizationCommand;
import com.example.clinic.organization.application.dto.DepartmentResponse;
import com.example.clinic.organization.application.dto.OpeningHoursDto;
import com.example.clinic.organization.application.dto.OrganizationResponse;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.BranchOpeningHours;
import com.example.clinic.organization.domain.Department;
import com.example.clinic.organization.domain.Organization;
import com.example.clinic.organization.infrastructure.BranchOpeningHoursRepository;
import com.example.clinic.organization.infrastructure.BranchRepository;
import com.example.clinic.organization.infrastructure.DepartmentRepository;
import com.example.clinic.organization.infrastructure.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service orchestrating organization, branch, opening hours, and department management.
 * Integrated with {@link AccessPolicy} and {@link SecurityContextFacade} to enforce strict tenant scoping
 * and eliminate Insecure Direct Object Reference (IDOR) vulnerabilities (WP-S04-08, FR-024, FR-026, TC-024, TC-026, TC-038).
 */
@Service
@Transactional(readOnly = true)
public class OrganizationAdminService {

    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final BranchOpeningHoursRepository branchOpeningHoursRepository;
    private final MembershipRepository membershipRepository;
    private final AccessPolicy accessPolicy;
    private final SecurityContextFacade securityContextFacade;

    public OrganizationAdminService(OrganizationRepository organizationRepository,
                                  BranchRepository branchRepository,
                                  DepartmentRepository departmentRepository,
                                  BranchOpeningHoursRepository branchOpeningHoursRepository,
                                  MembershipRepository membershipRepository,
                                  AccessPolicy accessPolicy,
                                  SecurityContextFacade securityContextFacade) {
        this.organizationRepository = organizationRepository;
        this.branchRepository = branchRepository;
        this.departmentRepository = departmentRepository;
        this.branchOpeningHoursRepository = branchOpeningHoursRepository;
        this.membershipRepository = membershipRepository;
        this.accessPolicy = accessPolicy;
        this.securityContextFacade = securityContextFacade;
    }

    // =========================================================================
    // 1. Organization Management (FR-001 / TC-001, FR-024 / TC-024)
    // =========================================================================

    /**
     * Creates a new organization under the authenticated actor's ownership.
     * Implements controlled bootstrap: the authenticated creator is immediately provisioned
     * as the organization-wide {@link Role#ROLE_ADMIN}.
     */
    @Transactional
    public OrganizationResponse createOrganization(CreateOrganizationCommand command) {
        Objects.requireNonNull(command, "CreateOrganizationCommand must not be null");
        UserAccount currentUser = securityContextFacade.getRequiredCurrentUser();

        Organization organization = new Organization(command.name());
        Organization saved = organizationRepository.save(organization);

        // Bootstrap creator as organization-wide administrator
        membershipRepository.save(new Membership(currentUser, saved, null, Role.ROLE_ADMIN));

        return mapToResponse(saved);
    }

    /**
     * Controlled administrative bootstrap provisioning outside public self-registration.
     */
    @Transactional
    public OrganizationResponse bootstrapOrganization(CreateOrganizationCommand command, UserAccount adminAccount) {
        Objects.requireNonNull(command, "CreateOrganizationCommand must not be null");
        Objects.requireNonNull(adminAccount, "Admin account must not be null");

        Organization organization = new Organization(command.name());
        Organization saved = organizationRepository.save(organization);

        membershipRepository.save(new Membership(adminAccount, saved, null, Role.ROLE_ADMIN));
        return mapToResponse(saved);
    }

    @Transactional
    public OrganizationResponse updateOrganization(UUID organizationId, String newName) {
        Objects.requireNonNull(organizationId, "Organization ID must not be null");
        accessPolicy.enforceAdminOrganizationAccess(organizationId, securityContextFacade.getRequiredCurrentUser());

        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("New organization name must not be blank");
        }
        Organization organization = findOrganizationOrThrow(organizationId);
        organization.setName(newName.strip());
        return mapToResponse(organization);
    }

    @Transactional
    public void activateOrganization(UUID organizationId) {
        Objects.requireNonNull(organizationId, "Organization ID must not be null");
        accessPolicy.enforceAdminOrganizationAccess(organizationId, securityContextFacade.getRequiredCurrentUser());

        Organization organization = findOrganizationOrThrow(organizationId);
        organization.activate();
    }

    @Transactional
    public void deactivateOrganization(UUID organizationId) {
        Objects.requireNonNull(organizationId, "Organization ID must not be null");
        accessPolicy.enforceAdminOrganizationAccess(organizationId, securityContextFacade.getRequiredCurrentUser());

        Organization organization = findOrganizationOrThrow(organizationId);
        organization.deactivate();
    }

    public OrganizationResponse getOrganization(UUID organizationId) {
        Objects.requireNonNull(organizationId, "Organization ID must not be null");
        accessPolicy.enforceAdminOrganizationAccess(organizationId, securityContextFacade.getRequiredCurrentUser());

        Organization organization = findOrganizationOrThrow(organizationId);
        return mapToResponse(organization);
    }

    // =========================================================================
    // 2. Branch Management (FR-002 / TC-002, FR-026 / TC-026)
    // =========================================================================

    @Transactional
    public BranchResponse createBranch(UUID organizationId, CreateBranchCommand command) {
        Objects.requireNonNull(organizationId, "Organization ID must not be null");
        Objects.requireNonNull(command, "CreateBranchCommand must not be null");
        accessPolicy.enforceAdminOrganizationAccess(organizationId, securityContextFacade.getRequiredCurrentUser());

        Organization organization = findOrganizationOrThrow(organizationId);
        if (!organization.isActive()) {
            throw new IllegalStateException("Cannot create branch under inactive organization: " + organizationId);
        }

        String timeZone = command.timeZone();
        if (timeZone == null || timeZone.isBlank()) {
            timeZone = "Europe/Istanbul";
        } else {
            try {
                ZoneId.of(timeZone);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid IANA time zone identifier: " + timeZone, e);
            }
        }

        if (command.address() != null && command.address().length() > 500) {
            throw new IllegalArgumentException("Address exceeds maximum length of 500 characters");
        }

        Branch branch = new Branch(organization, command.name(), timeZone, command.address());
        Branch saved = branchRepository.save(branch);
        return mapToResponse(saved);
    }

    public BranchResponse getBranch(UUID branchId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Branch branch = findBranchOrThrow(branchId);
        accessPolicy.enforceAdminBranchAccess(branchId, securityContextFacade.getRequiredCurrentUser());
        return mapToResponse(branch);
    }

    public List<BranchResponse> getBranchesByOrganization(UUID organizationId) {
        Objects.requireNonNull(organizationId, "Organization ID must not be null");
        accessPolicy.enforceAdminOrganizationAccess(organizationId, securityContextFacade.getRequiredCurrentUser());

        findOrganizationOrThrow(organizationId);
        return branchRepository.findByOrganizationId(organizationId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void activateBranch(UUID branchId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Branch branch = findBranchOrThrow(branchId);
        accessPolicy.enforceAdminBranchAccess(branchId, securityContextFacade.getRequiredCurrentUser());
        branch.setActive(true);
    }

    @Transactional
    public void deactivateBranch(UUID branchId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Branch branch = findBranchOrThrow(branchId);
        accessPolicy.enforceAdminBranchAccess(branchId, securityContextFacade.getRequiredCurrentUser());
        branch.setActive(false);
    }

    // =========================================================================
    // 3. Department Management (FR-004 / TC-004)
    // =========================================================================

    @Transactional
    public DepartmentResponse createDepartment(UUID branchId, String name) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Branch branch = findBranchOrThrow(branchId);
        accessPolicy.enforceAdminBranchAccess(branchId, securityContextFacade.getRequiredCurrentUser());

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Department name must not be blank");
        }
        if (!branch.isActive()) {
            throw new IllegalStateException("Cannot create department under inactive branch: " + branchId);
        }

        String trimmedName = name.strip();
        if (departmentRepository.findByBranchIdAndName(branchId, trimmedName).isPresent()) {
            throw new IllegalArgumentException("Department with name '" + trimmedName + "' already exists in branch: " + branchId);
        }

        Department department = new Department(branch, trimmedName);
        Department saved = departmentRepository.save(department);
        return mapToResponse(saved);
    }

    @Transactional
    public DepartmentResponse createDepartment(UUID branchId, CreateDepartmentCommand command) {
        Objects.requireNonNull(command, "CreateDepartmentCommand must not be null");
        return createDepartment(branchId, command.name());
    }

    public List<DepartmentResponse> getDepartmentsByBranch(UUID branchId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Branch branch = findBranchOrThrow(branchId);
        accessPolicy.enforceAdminBranchAccess(branchId, securityContextFacade.getRequiredCurrentUser());

        return departmentRepository.findByBranchId(branchId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    // =========================================================================
    // 4. Branch Opening Hours (FR-003 / TC-003)
    // =========================================================================

    /**
     * Atomically configures weekly opening hours for a branch, replacing any prior schedule.
     * Enforces administrator branch authorization before modifying persistent state.
     */
    @Transactional
    public List<OpeningHoursDto> configureOpeningHours(UUID branchId, List<OpeningHoursDto> hours) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Branch branch = findBranchOrThrow(branchId);
        accessPolicy.enforceAdminBranchAccess(branchId, securityContextFacade.getRequiredCurrentUser());

        if (!branch.isActive()) {
            throw new IllegalStateException("Cannot configure opening hours for inactive branch: " + branchId);
        }

        List<OpeningHoursDto> safeHours = hours != null ? hours : List.of();

        // Validate each item
        for (OpeningHoursDto dto : safeHours) {
            Objects.requireNonNull(dto, "OpeningHoursDto entry must not be null");
            if (dto.weekday() < 1 || dto.weekday() > 7) {
                throw new IllegalArgumentException("Weekday must be between 1 and 7, got: " + dto.weekday());
            }
            if (dto.startTime() == null || dto.endTime() == null) {
                throw new IllegalArgumentException("startTime and endTime must not be null");
            }
            if (!dto.startTime().isBefore(dto.endTime())) {
                throw new IllegalArgumentException(
                        "startTime (" + dto.startTime() + ") must precede endTime (" + dto.endTime() + ")"
                );
            }
        }

        // Validate no overlapping intervals on the same weekday
        Map<Short, List<OpeningHoursDto>> byWeekday = safeHours.stream()
                .collect(Collectors.groupingBy(OpeningHoursDto::weekday));

        for (Map.Entry<Short, List<OpeningHoursDto>> entry : byWeekday.entrySet()) {
            List<OpeningHoursDto> daySchedule = new ArrayList<>(entry.getValue());
            daySchedule.sort(Comparator.comparing(OpeningHoursDto::startTime));

            for (int i = 0; i < daySchedule.size() - 1; i++) {
                OpeningHoursDto current = daySchedule.get(i);
                OpeningHoursDto next = daySchedule.get(i + 1);

                if (current.endTime().isAfter(next.startTime())) {
                    throw new IllegalArgumentException(
                            "Overlapping opening hours on weekday " + entry.getKey() + ": [" +
                            current.startTime() + ", " + current.endTime() + ") overlaps with [" +
                            next.startTime() + ", " + next.endTime() + ")"
                    );
                }
            }
        }

        // Atomic replacement: remove previous schedule and persist the new valid schedule
        branchOpeningHoursRepository.deleteByBranchId(branchId);
        branchOpeningHoursRepository.flush();

        List<BranchOpeningHours> entities = safeHours.stream()
                .map(dto -> new BranchOpeningHours(branch, dto.weekday(), dto.startTime(), dto.endTime()))
                .toList();
        branchOpeningHoursRepository.saveAll(entities);

        return safeHours;
    }

    public List<OpeningHoursDto> getOpeningHoursByBranch(UUID branchId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Branch branch = findBranchOrThrow(branchId);
        accessPolicy.enforceAdminBranchAccess(branchId, securityContextFacade.getRequiredCurrentUser());

        return branchOpeningHoursRepository.findByBranchIdOrderByWeekdayAscStartTimeAsc(branchId).stream()
                .map(entity -> new OpeningHoursDto(entity.getWeekday(), entity.getStartTime(), entity.getEndTime()))
                .toList();
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private Organization findOrganizationOrThrow(UUID organizationId) {
        Objects.requireNonNull(organizationId, "Organization ID must not be null");
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));
    }

    private Branch findBranchOrThrow(UUID branchId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + branchId));
    }

    private OrganizationResponse mapToResponse(Organization org) {
        return new OrganizationResponse(org.getId(), org.getName(), org.isActive(), org.getCreatedAt());
    }

    private BranchResponse mapToResponse(Branch branch) {
        return new BranchResponse(
                branch.getId(),
                branch.getOrganization().getId(),
                branch.getName(),
                branch.getTimeZone(),
                branch.getAddress(),
                branch.isActive(),
                branch.getCreatedAt()
        );
    }

    private DepartmentResponse mapToResponse(Department dept) {
        return new DepartmentResponse(
                dept.getId(),
                dept.getBranch().getId(),
                dept.getName(),
                dept.isActive(),
                dept.getCreatedAt()
        );
    }
}
