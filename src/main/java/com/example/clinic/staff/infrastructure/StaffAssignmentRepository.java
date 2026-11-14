package com.example.clinic.staff.infrastructure;

import com.example.clinic.staff.domain.StaffAssignment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffAssignmentRepository extends JpaRepository<StaffAssignment, UUID> {

    @EntityGraph(attributePaths = {"staff", "staff.userAccount", "branch", "branch.organization", "department"})
    List<StaffAssignment> findByStaffId(UUID staffId);

    @EntityGraph(attributePaths = {"staff", "staff.userAccount", "branch", "branch.organization", "department"})
    List<StaffAssignment> findByBranchId(UUID branchId);

    @EntityGraph(attributePaths = {"staff", "staff.userAccount", "branch", "branch.organization", "department"})
    List<StaffAssignment> findByBranchIdInOrderByCreatedAtAsc(Collection<UUID> branchIds);

    List<StaffAssignment> findByStaffIdAndActiveTrue(UUID staffId);

    List<StaffAssignment> findByBranchIdAndActiveTrue(UUID branchId);

    Optional<StaffAssignment> findByStaffIdAndBranchIdAndActiveTrue(UUID staffId, UUID branchId);

    Optional<StaffAssignment> findByStaffIdAndBranchIdAndDepartmentId(UUID staffId, UUID branchId, UUID departmentId);

    boolean existsByStaffIdAndBranchIdAndDepartmentId(UUID staffId, UUID branchId, UUID departmentId);
}
