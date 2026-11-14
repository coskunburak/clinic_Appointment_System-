package com.example.clinic.catalog.infrastructure;

import com.example.clinic.catalog.domain.BranchService;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BranchServiceRepository extends JpaRepository<BranchService, UUID> {

    Optional<BranchService> findByBranchIdAndServiceDefinitionId(UUID branchId, UUID serviceDefinitionId);

    @EntityGraph(attributePaths = {"branch", "branch.organization", "serviceDefinition"})
    List<BranchService> findByBranchId(UUID branchId);

    @EntityGraph(attributePaths = {"branch", "branch.organization", "serviceDefinition"})
    List<BranchService> findByBranchIdAndActiveTrue(UUID branchId);

    @EntityGraph(attributePaths = {"branch", "branch.organization", "serviceDefinition"})
    @Query("SELECT bs FROM BranchService bs WHERE bs.id = :id")
    Optional<BranchService> findDetailedById(@Param("id") UUID id);

    List<BranchService> findByOrganizationId(UUID organizationId);
}
