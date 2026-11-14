package com.example.clinic.staff.infrastructure;

import com.example.clinic.staff.domain.StaffServiceEligibility;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffServiceEligibilityRepository extends JpaRepository<StaffServiceEligibility, UUID> {

    @EntityGraph(attributePaths = {
            "assignment", "assignment.staff", "assignment.department",
            "assignment.branch", "assignment.branch.organization",
            "branchService", "branchService.serviceDefinition"
    })
    List<StaffServiceEligibility> findByAssignmentId(UUID assignmentId);

    List<StaffServiceEligibility> findByAssignmentStaffId(UUID staffId);

    @EntityGraph(attributePaths = {
            "assignment", "assignment.staff", "assignment.department",
            "assignment.branch", "assignment.branch.organization",
            "branchService", "branchService.branch", "branchService.branch.organization", "branchService.serviceDefinition"
    })
    List<StaffServiceEligibility> findByBranchServiceId(UUID branchServiceId);

    Optional<StaffServiceEligibility> findByAssignmentIdAndBranchServiceId(UUID assignmentId, UUID branchServiceId);

    boolean existsByAssignmentIdAndBranchServiceId(UUID assignmentId, UUID branchServiceId);

    boolean existsByAssignmentStaffIdAndBranchServiceId(UUID staffId, UUID branchServiceId);

    void deleteByAssignmentId(UUID assignmentId);
}
