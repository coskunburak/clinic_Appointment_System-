package com.example.clinic.catalog.infrastructure;

import com.example.clinic.catalog.domain.ServiceDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceDefinitionRepository extends JpaRepository<ServiceDefinition, UUID> {

    Optional<ServiceDefinition> findByOrganizationIdAndName(UUID organizationId, String name);

    List<ServiceDefinition> findByOrganizationId(UUID organizationId);

    List<ServiceDefinition> findByOrganizationIdAndActiveTrue(UUID organizationId);
}
