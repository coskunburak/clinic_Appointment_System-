package com.example.clinic.catalog.application;

import com.example.clinic.catalog.application.dto.AssignBranchServiceCommand;
import com.example.clinic.catalog.application.dto.BranchServiceResponse;
import com.example.clinic.catalog.application.dto.CreateServiceCommand;
import com.example.clinic.catalog.application.dto.ServiceDefinitionResponse;
import com.example.clinic.catalog.application.dto.UpdateServiceDurationCommand;
import com.example.clinic.catalog.application.dto.UpdateServicePricingCommand;
import com.example.clinic.catalog.domain.BranchService;
import com.example.clinic.catalog.domain.ServiceDefinition;
import com.example.clinic.catalog.infrastructure.BranchServiceRepository;
import com.example.clinic.catalog.infrastructure.ServiceDefinitionRepository;
import com.example.clinic.common.domain.Money;
import com.example.clinic.common.security.SecurityContextFacade;
import com.example.clinic.identity.application.AccessPolicy;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.Organization;
import com.example.clinic.organization.infrastructure.BranchRepository;
import com.example.clinic.organization.infrastructure.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Application service managing organization-level service definitions and branch-specific service offerings.
 * Satisfies FR-005, FR-006, WP-S04-09, FR-024, and FR-026.
 * Enforces administrator authorization and tenant boundaries at the service layer.
 */
@Service
@Transactional(readOnly = true)
public class CatalogManagementService {

    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;
    private final ServiceDefinitionRepository serviceDefinitionRepository;
    private final BranchServiceRepository branchServiceRepository;
    private final AccessPolicy accessPolicy;
    private final SecurityContextFacade securityContextFacade;

    public CatalogManagementService(OrganizationRepository organizationRepository,
                                  BranchRepository branchRepository,
                                  ServiceDefinitionRepository serviceDefinitionRepository,
                                  BranchServiceRepository branchServiceRepository,
                                  AccessPolicy accessPolicy,
                                  SecurityContextFacade securityContextFacade) {
        this.organizationRepository = organizationRepository;
        this.branchRepository = branchRepository;
        this.serviceDefinitionRepository = serviceDefinitionRepository;
        this.branchServiceRepository = branchServiceRepository;
        this.accessPolicy = accessPolicy;
        this.securityContextFacade = securityContextFacade;
    }

    // =========================================================================
    // 1. Service Definition Management (FR-005 / TC-005)
    // =========================================================================

    @Transactional
    public ServiceDefinitionResponse defineService(UUID organizationId, CreateServiceCommand command) {
        Objects.requireNonNull(organizationId, "Organization ID must not be null");
        Objects.requireNonNull(command, "CreateServiceCommand must not be null");
        accessPolicy.enforceAdminOrganizationAccess(organizationId, securityContextFacade.getRequiredCurrentUser());

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));

        if (!organization.isActive()) {
            throw new IllegalStateException("Cannot define service under inactive organization: " + organizationId);
        }

        String trimmedName = command.name().strip();
        if (serviceDefinitionRepository.findByOrganizationIdAndName(organizationId, trimmedName).isPresent()) {
            throw new IllegalArgumentException(
                    "Service definition with name '" + trimmedName + "' already exists in organization: " + organizationId
            );
        }

        ServiceDefinition serviceDefinition = new ServiceDefinition(organization, trimmedName, command.description());
        ServiceDefinition saved = serviceDefinitionRepository.save(serviceDefinition);
        return mapToResponse(saved);
    }

    public ServiceDefinitionResponse getServiceDefinition(UUID serviceDefinitionId) {
        ServiceDefinition def = findServiceDefinitionOrThrow(serviceDefinitionId);
        accessPolicy.enforceAdminOrganizationAccess(def.getOrganization().getId(), securityContextFacade.getRequiredCurrentUser());
        return mapToResponse(def);
    }

    public List<ServiceDefinitionResponse> getServiceDefinitionsByOrganization(UUID organizationId) {
        Objects.requireNonNull(organizationId, "Organization ID must not be null");
        accessPolicy.enforceAdminOrganizationAccess(organizationId, securityContextFacade.getRequiredCurrentUser());

        return serviceDefinitionRepository.findByOrganizationId(organizationId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public ServiceDefinitionResponse updateServiceDefinition(UUID serviceDefinitionId, String newName, String newDescription) {
        ServiceDefinition def = findServiceDefinitionOrThrow(serviceDefinitionId);
        accessPolicy.enforceAdminOrganizationAccess(def.getOrganization().getId(), securityContextFacade.getRequiredCurrentUser());

        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("New service definition name must not be blank");
        }
        String trimmedName = newName.strip();
        serviceDefinitionRepository.findByOrganizationIdAndName(def.getOrganization().getId(), trimmedName)
                .filter(existing -> !existing.getId().equals(serviceDefinitionId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Service definition with name '" + trimmedName + "' already exists in organization: " + def.getOrganization().getId()
                    );
                });

        def.rename(trimmedName);
        def.changeDescription(newDescription);
        return mapToResponse(def);
    }

    @Transactional
    public void activateServiceDefinition(UUID serviceDefinitionId) {
        ServiceDefinition def = findServiceDefinitionOrThrow(serviceDefinitionId);
        accessPolicy.enforceAdminOrganizationAccess(def.getOrganization().getId(), securityContextFacade.getRequiredCurrentUser());
        def.activate();
    }

    @Transactional
    public void deactivateServiceDefinition(UUID serviceDefinitionId) {
        ServiceDefinition def = findServiceDefinitionOrThrow(serviceDefinitionId);
        accessPolicy.enforceAdminOrganizationAccess(def.getOrganization().getId(), securityContextFacade.getRequiredCurrentUser());
        def.deactivate();
    }

    // =========================================================================
    // 2. Branch Service Offering & Pricing Management (FR-006 / TC-006)
    // =========================================================================

    @Transactional
    public BranchServiceResponse assignServiceToBranch(UUID branchId,
                                                      UUID serviceDefinitionId,
                                                      int durationMinutes,
                                                      BigDecimal fee,
                                                      String currency) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(serviceDefinitionId, "ServiceDefinition ID must not be null");
        accessPolicy.enforceAdminBranchAccess(branchId, securityContextFacade.getRequiredCurrentUser());

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + branchId));

        ServiceDefinition serviceDefinition = serviceDefinitionRepository.findById(serviceDefinitionId)
                .orElseThrow(() -> new IllegalArgumentException("ServiceDefinition not found: " + serviceDefinitionId));

        // Enforce cross-tenant isolation
        if (!branch.getOrganization().getId().equals(serviceDefinition.getOrganization().getId())) {
            throw new IllegalArgumentException(
                    "Cross-tenant violation: Branch organization (" + branch.getOrganization().getId() +
                    ") does not match ServiceDefinition organization (" + serviceDefinition.getOrganization().getId() + ")"
            );
        }

        // Enforce active lifecycle invariants
        if (!branch.getOrganization().isActive()) {
            throw new IllegalStateException("Cannot assign service under inactive organization: " + branch.getOrganization().getId());
        }
        if (!branch.isActive()) {
            throw new IllegalStateException("Cannot assign service to inactive branch: " + branchId);
        }
        if (!serviceDefinition.isActive()) {
            throw new IllegalStateException("Cannot assign inactive service definition: " + serviceDefinitionId);
        }

        // Prevent duplicate offering in the same branch
        if (branchServiceRepository.findByBranchIdAndServiceDefinitionId(branchId, serviceDefinitionId).isPresent()) {
            throw new IllegalArgumentException(
                    "Branch (" + branchId + ") already offers service definition (" + serviceDefinitionId + ")"
            );
        }

        String validCurrencyCode = validateCurrencyCode(currency);
        Money price = Money.of(fee, Currency.getInstance(validCurrencyCode));

        BranchService branchService = new BranchService(branch, serviceDefinition, durationMinutes, price);
        BranchService saved = branchServiceRepository.save(branchService);
        return mapToResponse(saved);
    }

    @Transactional
    public BranchServiceResponse assignServiceToBranch(UUID branchId, AssignBranchServiceCommand command) {
        Objects.requireNonNull(command, "AssignBranchServiceCommand must not be null");
        return assignServiceToBranch(
                branchId,
                command.serviceDefinitionId(),
                command.durationMinutes(),
                command.fee(),
                command.currency()
        );
    }

    @Transactional
    public BranchServiceResponse updateServicePricing(UUID branchServiceId, BigDecimal newFee) {
        Objects.requireNonNull(branchServiceId, "BranchService ID must not be null");
        BranchService branchService = findBranchServiceOrThrow(branchServiceId);
        accessPolicy.enforceAdminBranchAccess(branchService.getBranch().getId(), securityContextFacade.getRequiredCurrentUser());

        branchService.changePrice(newFee);
        return mapToResponse(branchService);
    }

    @Transactional
    public BranchServiceResponse updateServicePricing(UUID branchServiceId, UpdateServicePricingCommand command) {
        Objects.requireNonNull(branchServiceId, "BranchService ID must not be null");
        Objects.requireNonNull(command, "UpdateServicePricingCommand must not be null");
        BranchService branchService = findBranchServiceOrThrow(branchServiceId);
        accessPolicy.enforceAdminBranchAccess(branchService.getBranch().getId(), securityContextFacade.getRequiredCurrentUser());

        if (command.currency() != null && !command.currency().isBlank()) {
            branchService.changePrice(command.newFee(), command.currency());
        } else {
            branchService.changePrice(command.newFee());
        }
        return mapToResponse(branchService);
    }

    @Transactional
    public BranchServiceResponse updateServiceDuration(UUID branchServiceId, int newDurationMinutes) {
        Objects.requireNonNull(branchServiceId, "BranchService ID must not be null");
        BranchService branchService = findBranchServiceOrThrow(branchServiceId);
        accessPolicy.enforceAdminBranchAccess(branchService.getBranch().getId(), securityContextFacade.getRequiredCurrentUser());

        branchService.changeDuration(newDurationMinutes);
        return mapToResponse(branchService);
    }

    @Transactional
    public BranchServiceResponse updateServiceDuration(UUID branchServiceId, UpdateServiceDurationCommand command) {
        Objects.requireNonNull(command, "UpdateServiceDurationCommand must not be null");
        return updateServiceDuration(branchServiceId, command.newDurationMinutes());
    }

    public BranchServiceResponse getBranchService(UUID branchServiceId) {
        BranchService branchService = findBranchServiceOrThrow(branchServiceId);
        accessPolicy.enforceAdminBranchAccess(branchService.getBranch().getId(), securityContextFacade.getRequiredCurrentUser());
        return mapToResponse(branchService);
    }

    public List<BranchServiceResponse> getBranchServicesByBranch(UUID branchId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        accessPolicy.enforceAdminBranchAccess(branchId, securityContextFacade.getRequiredCurrentUser());

        return branchServiceRepository.findByBranchId(branchId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void activateBranchService(UUID branchServiceId) {
        BranchService branchService = findBranchServiceOrThrow(branchServiceId);
        accessPolicy.enforceAdminBranchAccess(branchService.getBranch().getId(), securityContextFacade.getRequiredCurrentUser());
        branchService.activate();
    }

    @Transactional
    public void deactivateBranchService(UUID branchServiceId) {
        BranchService branchService = findBranchServiceOrThrow(branchServiceId);
        accessPolicy.enforceAdminBranchAccess(branchService.getBranch().getId(), securityContextFacade.getRequiredCurrentUser());
        branchService.deactivate();
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private ServiceDefinition findServiceDefinitionOrThrow(UUID id) {
        Objects.requireNonNull(id, "ServiceDefinition ID must not be null");
        return serviceDefinitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ServiceDefinition not found: " + id));
    }

    private BranchService findBranchServiceOrThrow(UUID id) {
        Objects.requireNonNull(id, "BranchService ID must not be null");
        return branchServiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("BranchService not found: " + id));
    }

    private String validateCurrencyCode(String code) {
        if (code == null || code.isBlank() || code.strip().length() != 3) {
            throw new IllegalArgumentException("Currency code must be a 3-character ISO code: " + code);
        }
        return code.strip().toUpperCase(Locale.ROOT);
    }

    private ServiceDefinitionResponse mapToResponse(ServiceDefinition def) {
        return new ServiceDefinitionResponse(
                def.getId(),
                def.getOrganization().getId(),
                def.getName(),
                def.getDescription(),
                def.isActive(),
                def.getCreatedAt()
        );
    }

    private BranchServiceResponse mapToResponse(BranchService bs) {
        return new BranchServiceResponse(
                bs.getId(),
                bs.getOrganization().getId(),
                bs.getBranch().getId(),
                bs.getServiceDefinition().getId(),
                bs.getServiceDefinition().getName(),
                bs.getDurationMinutes(),
                bs.getFeeAmount(),
                bs.getCurrency(),
                bs.isActive(),
                bs.getCreatedAt()
        );
    }
}
