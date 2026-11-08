package com.example.clinic.staff.domain;

import com.example.clinic.catalog.domain.BranchService;
import com.example.clinic.common.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity mapping a staff assignment to an authorized medical service at that specific branch.
 * Fulfills FR-009, STA-02, and TC-009.
 */
@Entity
@Table(name = "staff_service_eligibility")
public class StaffServiceEligibility extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private StaffAssignment assignment;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_service_id", nullable = false)
    private BranchService branchService;

    protected StaffServiceEligibility() {
        super();
    }

    public StaffServiceEligibility(StaffAssignment assignment, BranchService branchService) {
        this(UUID.randomUUID(), assignment, branchService, Instant.now());
    }

    public StaffServiceEligibility(UUID id, StaffAssignment assignment, BranchService branchService, Instant createdAt) {
        super(id, createdAt);
        this.assignment = Objects.requireNonNull(assignment, "assignment must not be null");
        this.branchService = Objects.requireNonNull(branchService, "branchService must not be null");
        validateServiceBelongsToAssignmentBranch(this.assignment, this.branchService);
    }

    public static void validateServiceBelongsToAssignmentBranch(StaffAssignment assignment, BranchService branchService) {
        if (!branchService.getBranch().getId().equals(assignment.getBranch().getId())) {
            throw new IllegalArgumentException(
                    "BranchService " + branchService.getId() + " belongs to branch " + branchService.getBranch().getId() +
                            ", which does not match assignment branch " + assignment.getBranch().getId()
            );
        }
    }

    public StaffAssignment getAssignment() {
        return assignment;
    }

    public BranchService getBranchService() {
        return branchService;
    }

    @Override
    public String toString() {
        return "StaffServiceEligibility{" +
                "id=" + getId() +
                ", assignmentId=" + (assignment != null ? assignment.getId() : null) +
                ", branchServiceId=" + (branchService != null ? branchService.getId() : null) +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}
