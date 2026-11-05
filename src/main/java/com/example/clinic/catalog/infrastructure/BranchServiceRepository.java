package com.example.clinic.catalog.infrastructure;

import com.example.clinic.catalog.domain.BranchService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BranchServiceRepository extends JpaRepository<BranchService, UUID> {

    Optional<BranchService> findByBranchIdAndServiceDefinitionId(UUID branchId, UUID serviceDefinitionId);

    List<BranchService> findByBranchId(UUID branchId);

    List<BranchService> findByBranchIdAndActiveTrue(UUID branchId);

    List<BranchService> findByOrganizationId(UUID organizationId);
}
