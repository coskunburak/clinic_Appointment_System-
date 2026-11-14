package com.example.clinic.identity.application;

import com.example.clinic.identity.domain.Membership;
import com.example.clinic.identity.domain.PatientProfile;
import com.example.clinic.identity.domain.Role;
import com.example.clinic.identity.domain.StaffProfile;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.MembershipRepository;
import com.example.clinic.identity.infrastructure.PatientProfileRepository;
import com.example.clinic.identity.infrastructure.StaffProfileRepository;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.infrastructure.BranchRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Service-layer multi-tenant access policy and IDOR defense component fulfilling WP-S04-07.
 * Enforces authoritative ownership, tenant boundaries, and branch scoping directly at the service layer,
 * regardless of whether the service is invoked through MVC controllers or internal application boundaries.
 * <p>
 * Key capabilities:
 * <ul>
 *   <li>Patient self-access: patients can only access profiles they own.</li>
 *   <li>Staff branch scoping: staff can only operate within explicitly assigned branches.</li>
 *   <li>Admin organization scoping: administrators can only manage organizations and branches authorized by membership.</li>
 * </ul>
 */
@Component
@Transactional(readOnly = true)
public class AccessPolicy {

    private final UserAccountRepository userAccountRepository;
    private final MembershipRepository membershipRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final BranchRepository branchRepository;

    public AccessPolicy(UserAccountRepository userAccountRepository,
                        MembershipRepository membershipRepository,
                        PatientProfileRepository patientProfileRepository,
                        StaffProfileRepository staffProfileRepository,
                        BranchRepository branchRepository) {
        this.userAccountRepository = userAccountRepository;
        this.membershipRepository = membershipRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.branchRepository = branchRepository;
    }

    /**
     * Enforces that the current authenticated user owns the target patient profile.
     * Prevents Insecure Direct Object Reference (IDOR) attacks across patients (FR-022, TC-022).
     */
    public void enforcePatientSelfAccess(UUID patientId, UserAccount currentUser) {
        Objects.requireNonNull(patientId, "patientId must not be null");
        Objects.requireNonNull(currentUser, "currentUser must not be null");

        validateUserEnabled(currentUser);

        PatientProfile profile = patientProfileRepository.findById(patientId)
                .orElseThrow(() -> new AccessDeniedException("Access denied: patient profile not found"));

        if (!profile.getUserAccount().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Access denied: actor is not the owner of patient profile " + patientId);
        }
    }

    public void enforcePatientSelfAccess(UUID patientId, UUID userId) {
        enforcePatientSelfAccess(patientId, resolveUserOrThrow(userId));
    }

    /**
     * Enforces that the current staff member is authorized to operate within the given branch.
     * Prevents cross-branch operations and unauthorized staff actions (FR-023, TC-023).
     */
    public void enforceStaffBranchAccess(UUID branchId, UserAccount currentStaff) {
        Objects.requireNonNull(branchId, "branchId must not be null");
        Objects.requireNonNull(currentStaff, "currentStaff must not be null");

        validateUserEnabled(currentStaff);

        // Verify active staff profile if present
        staffProfileRepository.findByUserAccountId(currentStaff.getId()).ifPresent(profile -> {
            if (!profile.isActive()) {
                throw new AccessDeniedException("Access denied: staff profile is inactive for user " + currentStaff.getId());
            }
        });

        List<Membership> memberships = membershipRepository.findByUserAccountIdAndRole(currentStaff.getId(), Role.ROLE_STAFF);
        boolean authorized = memberships.stream()
                .anyMatch(m -> m.getBranch() != null && m.getBranch().getId().equals(branchId));

        if (!authorized) {
            throw new AccessDeniedException("Access denied: staff member is not assigned to branch " + branchId);
        }
    }

    public void enforceStaffBranchAccess(UUID branchId, UUID userId) {
        enforceStaffBranchAccess(branchId, resolveUserOrThrow(userId));
    }

    /**
     * Enforces that the administrator is authorized for the target organization.
     * Organization-wide admin memberships (branch_id = null) are permitted; branch-scoped admin memberships
     * cannot perform organization-wide operations (FR-024, FR-026, TC-026).
     */
    public void enforceAdminOrganizationAccess(UUID orgId, UserAccount currentAdmin) {
        Objects.requireNonNull(orgId, "organizationId must not be null");
        Objects.requireNonNull(currentAdmin, "currentAdmin must not be null");

        validateUserEnabled(currentAdmin);

        List<Membership> adminMemberships = membershipRepository.findByUserAccountIdAndRole(currentAdmin.getId(), Role.ROLE_ADMIN);

        List<Membership> orgMemberships = adminMemberships.stream()
                .filter(m -> m.getOrganization().getId().equals(orgId))
                .toList();

        if (orgMemberships.isEmpty()) {
            throw new AccessDeniedException("Access denied: administrator is not authorized for organization " + orgId);
        }

        boolean hasOrgWideScope = orgMemberships.stream().anyMatch(m -> m.getBranch() == null);
        if (!hasOrgWideScope) {
            throw new AccessDeniedException(
                    "Access denied: administrator membership is restricted to specific branch, cannot perform organization-level operation on " + orgId
            );
        }
    }

    public void enforceAdminOrganizationAccess(UUID orgId, UUID userId) {
        enforceAdminOrganizationAccess(orgId, resolveUserOrThrow(userId));
    }

    /**
     * Checks whether an administrator has authority over a specific branch (either through organization-wide
     * admin membership or branch-specific admin membership).
     * Returns true if authorized, false otherwise.
     */
    public boolean hasAdminBranchAccess(UUID branchId, UserAccount currentAdmin) {
        if (branchId == null || currentAdmin == null || !currentAdmin.isEnabled()) {
            return false;
        }

        Branch branch = branchRepository.findById(branchId).orElse(null);
        if (branch == null) {
            return false;
        }

        UUID orgId = branch.getOrganization().getId();
        List<Membership> adminMemberships = membershipRepository.findByUserAccountIdAndRole(currentAdmin.getId(), Role.ROLE_ADMIN);

        return adminMemberships.stream()
                .anyMatch(m -> m.getOrganization().getId().equals(orgId) &&
                        (m.getBranch() == null || m.getBranch().getId().equals(branchId)));
    }

    public boolean hasAdminBranchAccess(UUID branchId, UUID userId) {
        if (userId == null) {
            return false;
        }
        return userAccountRepository.findById(userId)
                .map(u -> hasAdminBranchAccess(branchId, u))
                .orElse(false);
    }

    /**
     * Enforces that the administrator has authority over a specific branch (either through organization-wide
     * admin membership or branch-specific admin membership) (FR-024, TC-024, TC-026).
     */
    public void enforceAdminBranchAccess(UUID branchId, UserAccount currentAdmin) {
        Objects.requireNonNull(branchId, "branchId must not be null");
        Objects.requireNonNull(currentAdmin, "currentAdmin must not be null");

        validateUserEnabled(currentAdmin);

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new AccessDeniedException("Access denied: branch not found: " + branchId));

        UUID orgId = branch.getOrganization().getId();

        List<Membership> adminMemberships = membershipRepository.findByUserAccountIdAndRole(currentAdmin.getId(), Role.ROLE_ADMIN);

        boolean authorized = adminMemberships.stream()
                .anyMatch(m -> m.getOrganization().getId().equals(orgId) &&
                        (m.getBranch() == null || m.getBranch().getId().equals(branchId)));

        if (!authorized) {
            throw new AccessDeniedException("Access denied: administrator is not authorized for branch " + branchId);
        }
    }

    public void enforceAdminBranchAccess(UUID branchId, UUID userId) {
        enforceAdminBranchAccess(branchId, resolveUserOrThrow(userId));
    }

    /**
     * Resolves all branch IDs over which the authenticated administrator holds authority
     * (either via organization-wide or branch-specific {@link Role#ROLE_ADMIN} membership).
     */
    public List<UUID> getAdminAuthorizedBranchIds(UserAccount currentAdmin) {
        Objects.requireNonNull(currentAdmin, "currentAdmin must not be null");
        validateUserEnabled(currentAdmin);

        List<Membership> adminMemberships = membershipRepository.findByUserAccountIdAndRole(currentAdmin.getId(), Role.ROLE_ADMIN);
        Set<UUID> branchIds = new LinkedHashSet<>();

        for (Membership membership : adminMemberships) {
            if (membership.getBranch() != null) {
                branchIds.add(membership.getBranch().getId());
            } else if (membership.getOrganization() != null) {
                branchRepository.findByOrganizationId(membership.getOrganization().getId())
                        .forEach(branch -> branchIds.add(branch.getId()));
            }
        }
        return new ArrayList<>(branchIds);
    }

    /**
     * Resolves all organization IDs over which the authenticated administrator holds
     * organization-wide ({@code branch == null}) authority.
     */
    public List<UUID> getAdminAuthorizedOrganizationWideIds(UserAccount currentAdmin) {
        Objects.requireNonNull(currentAdmin, "currentAdmin must not be null");
        validateUserEnabled(currentAdmin);

        return membershipRepository.findByUserAccountIdAndRole(currentAdmin.getId(), Role.ROLE_ADMIN).stream()
                .filter(m -> m.getBranch() == null && m.getOrganization() != null)
                .map(m -> m.getOrganization().getId())
                .distinct()
                .toList();
    }

    private void validateUserEnabled(UserAccount user) {
        if (!user.isEnabled()) {
            throw new AccessDeniedException("Access denied: user account is disabled");
        }
    }

    private UserAccount resolveUserOrThrow(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("Access denied: user account not found"));
    }
}
