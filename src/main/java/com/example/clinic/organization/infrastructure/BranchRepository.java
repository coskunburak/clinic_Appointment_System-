package com.example.clinic.organization.infrastructure;

import com.example.clinic.organization.domain.Branch;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BranchRepository extends JpaRepository<Branch, UUID> {

    List<Branch> findByOrganizationId(UUID organizationId);

    List<Branch> findByOrganizationIdAndActiveTrue(UUID organizationId);

    @EntityGraph(attributePaths = {"organization"})
    List<Branch> findByActiveTrue();

    Optional<Branch> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
