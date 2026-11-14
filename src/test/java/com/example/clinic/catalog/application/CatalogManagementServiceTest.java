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
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.Organization;
import com.example.clinic.organization.infrastructure.BranchRepository;
import com.example.clinic.organization.infrastructure.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogManagementServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private ServiceDefinitionRepository serviceDefinitionRepository;

    @Mock
    private BranchServiceRepository branchServiceRepository;

    @Mock
    private AccessPolicy accessPolicy;

    @Mock
    private SecurityContextFacade securityContextFacade;

    @InjectMocks
    private CatalogManagementService catalogManagementService;

    private Organization activeOrg;
    private Branch activeBranch;
    private ServiceDefinition activeServiceDef;
    private UserAccount adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new UserAccount("admin@clinic.test", "hash123");
        activeOrg = new Organization("Şehir Tıp Grubu");
        activeBranch = new Branch(activeOrg, "Kadıköy Şubesi", "Europe/Istanbul", "Bağdat Cad.");
        activeServiceDef = new ServiceDefinition(activeOrg, "Genel Muayene", "Rutin poliklinik kontrolü");
        org.mockito.Mockito.lenient().when(securityContextFacade.getRequiredCurrentUser()).thenReturn(adminUser);
    }

    // =========================================================================
    // 1. Service Definition Tests (FR-005 / TC-005)
    // =========================================================================

    @Test
    @DisplayName("Should define new service definition under active organization")
    void shouldDefineService() {
        when(organizationRepository.findById(activeOrg.getId())).thenReturn(Optional.of(activeOrg));
        when(serviceDefinitionRepository.findByOrganizationIdAndName(activeOrg.getId(), "Kardiyoloji Muayenesi"))
                .thenReturn(Optional.empty());
        when(serviceDefinitionRepository.save(any(ServiceDefinition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceDefinitionResponse response = catalogManagementService.defineService(
                activeOrg.getId(),
                new CreateServiceCommand("Kardiyoloji Muayenesi", "Kalp ve damar muayenesi")
        );

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Kardiyoloji Muayenesi");
        assertThat(response.description()).isEqualTo("Kalp ve damar muayenesi");
        assertThat(response.organizationId()).isEqualTo(activeOrg.getId());
        verify(serviceDefinitionRepository).save(any(ServiceDefinition.class));
    }

    @Test
    @DisplayName("Negative: should reject duplicate service name within the same organization")
    void shouldRejectDuplicateServiceNameInSameOrg() {
        when(organizationRepository.findById(activeOrg.getId())).thenReturn(Optional.of(activeOrg));
        when(serviceDefinitionRepository.findByOrganizationIdAndName(activeOrg.getId(), "Genel Muayene"))
                .thenReturn(Optional.of(activeServiceDef));

        assertThatThrownBy(() -> catalogManagementService.defineService(
                activeOrg.getId(),
                new CreateServiceCommand("Genel Muayene", "Farklı açıklama")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists in organization");

        verify(serviceDefinitionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Negative: should reject defining service under inactive organization")
    void shouldRejectServiceUnderInactiveOrg() {
        activeOrg.deactivate();
        when(organizationRepository.findById(activeOrg.getId())).thenReturn(Optional.of(activeOrg));

        assertThatThrownBy(() -> catalogManagementService.defineService(
                activeOrg.getId(),
                new CreateServiceCommand("Laboratuvar", "Açıklama")
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot define service under inactive organization");
    }

    // =========================================================================
    // 2. Branch Service Assignment Tests (FR-006 / TC-006)
    // =========================================================================

    @Test
    @DisplayName("Should assign service definition to branch with duration and price")
    void shouldAssignServiceToBranch() {
        when(branchRepository.findById(activeBranch.getId())).thenReturn(Optional.of(activeBranch));
        when(serviceDefinitionRepository.findById(activeServiceDef.getId())).thenReturn(Optional.of(activeServiceDef));
        when(branchServiceRepository.findByBranchIdAndServiceDefinitionId(activeBranch.getId(), activeServiceDef.getId()))
                .thenReturn(Optional.empty());
        when(branchServiceRepository.save(any(BranchService.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BranchServiceResponse response = catalogManagementService.assignServiceToBranch(
                activeBranch.getId(),
                new AssignBranchServiceCommand(activeServiceDef.getId(), 30, new BigDecimal("1200.00"), "TRY")
        );

        assertThat(response).isNotNull();
        assertThat(response.branchId()).isEqualTo(activeBranch.getId());
        assertThat(response.serviceDefinitionId()).isEqualTo(activeServiceDef.getId());
        assertThat(response.durationMinutes()).isEqualTo(30);
        assertThat(response.feeAmount()).isEqualByComparingTo("1200.00");
        assertThat(response.currency()).isEqualTo("TRY");
        verify(branchServiceRepository).save(any(BranchService.class));
    }

    @Test
    @DisplayName("Negative: should reject cross-tenant service assignment between different organizations")
    void shouldRejectCrossTenantAssignment() {
        Organization otherOrg = new Organization("Başka Sağlık Grubu");
        ServiceDefinition otherDef = new ServiceDefinition(otherOrg, "Farklı Hizmet", "Açıklama");

        when(branchRepository.findById(activeBranch.getId())).thenReturn(Optional.of(activeBranch));
        when(serviceDefinitionRepository.findById(otherDef.getId())).thenReturn(Optional.of(otherDef));

        assertThatThrownBy(() -> catalogManagementService.assignServiceToBranch(
                activeBranch.getId(),
                new AssignBranchServiceCommand(otherDef.getId(), 30, new BigDecimal("800.00"), "TRY")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cross-tenant violation");

        verify(branchServiceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Negative: should reject duplicate assignment of same service definition to branch")
    void shouldRejectDuplicateBranchServiceAssignment() {
        when(branchRepository.findById(activeBranch.getId())).thenReturn(Optional.of(activeBranch));
        when(serviceDefinitionRepository.findById(activeServiceDef.getId())).thenReturn(Optional.of(activeServiceDef));

        BranchService existing = new BranchService(
                activeBranch, activeServiceDef, 30,
                Money.of(new BigDecimal("800.00"), Currency.getInstance("TRY"))
        );
        when(branchServiceRepository.findByBranchIdAndServiceDefinitionId(activeBranch.getId(), activeServiceDef.getId()))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> catalogManagementService.assignServiceToBranch(
                activeBranch.getId(),
                new AssignBranchServiceCommand(activeServiceDef.getId(), 45, new BigDecimal("1000.00"), "TRY")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already offers service definition");

        verify(branchServiceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Negative: should reject assignment when service definition is inactive")
    void shouldRejectAssignmentWhenServiceDefInactive() {
        activeServiceDef.deactivate();

        when(branchRepository.findById(activeBranch.getId())).thenReturn(Optional.of(activeBranch));
        when(serviceDefinitionRepository.findById(activeServiceDef.getId())).thenReturn(Optional.of(activeServiceDef));

        assertThatThrownBy(() -> catalogManagementService.assignServiceToBranch(
                activeBranch.getId(),
                new AssignBranchServiceCommand(activeServiceDef.getId(), 30, new BigDecimal("800.00"), "TRY")
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot assign inactive service definition");
    }

    @Test
    @DisplayName("Negative: should reject assignment when branch is inactive")
    void shouldRejectAssignmentWhenBranchInactive() {
        activeBranch.setActive(false);

        when(branchRepository.findById(activeBranch.getId())).thenReturn(Optional.of(activeBranch));
        when(serviceDefinitionRepository.findById(activeServiceDef.getId())).thenReturn(Optional.of(activeServiceDef));

        assertThatThrownBy(() -> catalogManagementService.assignServiceToBranch(
                activeBranch.getId(),
                new AssignBranchServiceCommand(activeServiceDef.getId(), 30, new BigDecimal("800.00"), "TRY")
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot assign service to inactive branch");
    }

    // =========================================================================
    // 3. Price and Duration Updates (FR-006 / TC-006)
    // =========================================================================

    @Test
    @DisplayName("Should update branch service pricing preserving existing currency")
    void shouldUpdateBranchServicePrice() {
        BranchService branchService = new BranchService(
                activeBranch, activeServiceDef, 30,
                Money.of(new BigDecimal("1000.00"), Currency.getInstance("TRY"))
        );
        when(branchServiceRepository.findById(branchService.getId())).thenReturn(Optional.of(branchService));

        BranchServiceResponse response = catalogManagementService.updateServicePricing(
                branchService.getId(),
                new UpdateServicePricingCommand(new BigDecimal("1350.00"))
        );

        assertThat(response.feeAmount()).isEqualByComparingTo("1350.00");
        assertThat(response.currency()).isEqualTo("TRY");
    }

    @Test
    @DisplayName("Negative: should reject negative price update in command")
    void shouldRejectNegativePriceInCommand() {
        assertThatThrownBy(() -> new UpdateServicePricingCommand(new BigDecimal("-10.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be negative");
    }

    @Test
    @DisplayName("Should update branch service duration")
    void shouldUpdateBranchServiceDuration() {
        BranchService branchService = new BranchService(
                activeBranch, activeServiceDef, 30,
                Money.of(new BigDecimal("1000.00"), Currency.getInstance("TRY"))
        );
        when(branchServiceRepository.findById(branchService.getId())).thenReturn(Optional.of(branchService));

        BranchServiceResponse response = catalogManagementService.updateServiceDuration(
                branchService.getId(),
                new UpdateServiceDurationCommand(60)
        );

        assertThat(response.durationMinutes()).isEqualTo(60);
    }

    @Test
    @DisplayName("Should update service definition name/description, reject duplicate name, and toggle active status")
    void shouldUpdateAndToggleServiceDefinition() {
        ServiceDefinition otherDef = new ServiceDefinition(activeOrg, "MR Çekimi", "Radyoloji");
        when(serviceDefinitionRepository.findById(activeServiceDef.getId())).thenReturn(Optional.of(activeServiceDef));
        when(serviceDefinitionRepository.findByOrganizationIdAndName(activeOrg.getId(), "Güncel Muayene"))
                .thenReturn(Optional.empty());
        when(serviceDefinitionRepository.findByOrganizationIdAndName(activeOrg.getId(), "MR Çekimi"))
                .thenReturn(Optional.of(otherDef));

        ServiceDefinitionResponse updated = catalogManagementService.updateServiceDefinition(
                activeServiceDef.getId(), "Güncel Muayene", "Yenilenmiş kapsam"
        );
        assertThat(updated.name()).isEqualTo("Güncel Muayene");
        assertThat(updated.description()).isEqualTo("Yenilenmiş kapsam");

        assertThatThrownBy(() -> catalogManagementService.updateServiceDefinition(
                activeServiceDef.getId(), "MR Çekimi", "Çakışan isim"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        catalogManagementService.deactivateServiceDefinition(activeServiceDef.getId());
        assertThat(activeServiceDef.isActive()).isFalse();

        catalogManagementService.activateServiceDefinition(activeServiceDef.getId());
        assertThat(activeServiceDef.isActive()).isTrue();
    }
}
