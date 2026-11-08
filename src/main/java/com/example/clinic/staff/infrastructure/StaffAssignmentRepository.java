package com.example.clinic.staff.infrastructure;

import com.example.clinic.staff.domain.StaffAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffAssignmentRepository extends JpaRepository<StaffAssignment, UUID> {

    List<StaffAssignment> findByStaffId(UUID staffId);

    List<StaffAssignment> findByBranchId(UUID branchId);

    List<StaffAssignment> findByStaffIdAndActiveTrue(UUID staffId);

    List<StaffAssignment> findByBranchIdAndActiveTrue(UUID branchId);

    Optional<StaffAssignment> findByStaffIdAndBranchIdAndActiveTrue(UUID staffId, UUID branchId);

    Optional<StaffAssignment> findByStaffIdAndBranchIdAndDepartmentId(UUID staffId, UUID branchId, UUID departmentId);

    boolean existsByStaffIdAndBranchIdAndDepartmentId(UUID staffId, UUID branchId, UUID departmentId);
}
