package com.example.clinic.organization.infrastructure;

import com.example.clinic.organization.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    List<Department> findByBranchId(UUID branchId);
    Optional<Department> findByBranchIdAndName(UUID branchId, String name);
}
