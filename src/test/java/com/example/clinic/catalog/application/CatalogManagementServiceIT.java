package com.example.clinic.catalog.application;

import com.example.clinic.AbstractIntegrationTest;
import com.example.clinic.catalog.application.dto.AssignBranchServiceCommand;
import com.example.clinic.catalog.application.dto.BranchServiceResponse;
import com.example.clinic.catalog.application.dto.CreateServiceCommand;
import com.example.clinic.catalog.application.dto.ServiceDefinitionResponse;
import com.example.clinic.catalog.application.dto.UpdateServicePricingCommand;
import com.example.clinic.common.security.ClinicPrincipal;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import com.example.clinic.organization.application.OrganizationAdminService;
import com.example.clinic.organization.application.dto.BranchResponse;
import com.example.clinic.organization.application.dto.CreateBranchCommand;
import com.example.clinic.organization.application.dto.CreateOrganizationCommand;
import com.example.clinic.organization.application.dto.OrganizationResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class CatalogManagementServiceIT extends AbstractIntegrationTest {

    @Autowired
    private OrganizationAdminService organizationAdminService;

    @Autowired
    private CatalogManagementService catalogManagementService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private UserAccount testAdmin;

    @BeforeEach
    void setUpSecurityActor() {
        testAdmin = userAccountRepository.saveAndFlush(
                new UserAccount("cat_admin_" + java.util.UUID.randomUUID() + "@clinic.test", "hash123")
        );
        ClinicPrincipal principal = new ClinicPrincipal(
                testAdmin.getId(),
                testAdmin.getEmail(),
                testAdmin.getPasswordHash(),
                testAdmin.isEnabled(),
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("TC-005 / FR-005: Define medical service in catalog at organization level and retrieve")
    void shouldDefineAndRetrieveOrganizationService() {
        OrganizationResponse org = organizationAdminService.createOrganization(
                new CreateOrganizationCommand("Medicana Sağlık")
        );

        ServiceDefinitionResponse def = catalogManagementService.defineService(
                org.id(),
                new CreateServiceCommand("Genel Muayene", "Rutin poliklinik kontrolü")
        );

        assertThat(def.id()).isNotNull();
        assertThat(def.name()).isEqualTo("Genel Muayene");
        assertThat(def.organizationId()).isEqualTo(org.id());
        assertThat(def.active()).isTrue();

        List<ServiceDefinitionResponse> orgServices = catalogManagementService.getServiceDefinitionsByOrganization(org.id());
        assertThat(orgServices).hasSize(1);
        assertThat(orgServices.getFirst().name()).isEqualTo("Genel Muayene");
    }

    @Test
    @DisplayName("TC-006 / FR-006: Allow different pricing and durations for same service across different branches")
    void shouldAllowDifferentPricingAndDurationsAcrossBranches() {
        OrganizationResponse org = organizationAdminService.createOrganization(
                new CreateOrganizationCommand("Acıbadem Grubu")
        );
        BranchResponse branchKadikoy = organizationAdminService.createBranch(
                org.id(), new CreateBranchCommand("Kadıköy Şubesi", "Europe/Istanbul", "Bağdat Cad.")
        );
        BranchResponse branchNisantasi = organizationAdminService.createBranch(
                org.id(), new CreateBranchCommand("Nişantaşı Şubesi", "Europe/Istanbul", "Valikonağı Cad.")
        );

        ServiceDefinitionResponse serviceDef = catalogManagementService.defineService(
                org.id(), new CreateServiceCommand("Genel Muayene", "Rutin muayene")
        );

        // Kadıköy: 30 minutes, 1500.00 TRY
        BranchServiceResponse kadikoyOffering = catalogManagementService.assignServiceToBranch(
                branchKadikoy.id(),
                new AssignBranchServiceCommand(serviceDef.id(), 30, new BigDecimal("1500.00"), "TRY")
        );

        // Nişantaşı: 45 minutes, 2500.00 TRY
        BranchServiceResponse nisantasiOffering = catalogManagementService.assignServiceToBranch(
                branchNisantasi.id(),
                new AssignBranchServiceCommand(serviceDef.id(), 45, new BigDecimal("2500.00"), "TRY")
        );

        assertThat(kadikoyOffering.durationMinutes()).isEqualTo(30);
        assertThat(kadikoyOffering.feeAmount()).isEqualByComparingTo("1500.00");
        assertThat(kadikoyOffering.currency()).isEqualTo("TRY");

        assertThat(nisantasiOffering.durationMinutes()).isEqualTo(45);
        assertThat(nisantasiOffering.feeAmount()).isEqualByComparingTo("2500.00");
        assertThat(nisantasiOffering.currency()).isEqualTo("TRY");

        // Update Kadıköy pricing to 1800.00 TRY
        BranchServiceResponse updatedKadikoy = catalogManagementService.updateServicePricing(
                kadikoyOffering.id(),
                new UpdateServicePricingCommand(new BigDecimal("1800.00"))
        );

        assertThat(updatedKadikoy.feeAmount()).isEqualByComparingTo("1800.00");

        // Verify Nişantaşı pricing is completely unaffected (independent branch pricing)
        BranchServiceResponse currentNisantasi = catalogManagementService.getBranchService(nisantasiOffering.id());
        assertThat(currentNisantasi.feeAmount()).isEqualByComparingTo("2500.00");
        assertThat(currentNisantasi.durationMinutes()).isEqualTo(45);

        // Verify organization-level ServiceDefinition is untouched
        ServiceDefinitionResponse currentServiceDef = catalogManagementService.getServiceDefinition(serviceDef.id());
        assertThat(currentServiceDef.name()).isEqualTo("Genel Muayene");
    }

    @Test
    @DisplayName("Negative: should reject cross-organization service assignment at application level")
    void shouldRejectCrossOrgServiceAssignment() {
        OrganizationResponse orgA = organizationAdminService.createOrganization(
                new CreateOrganizationCommand("Hastane A")
        );
        OrganizationResponse orgB = organizationAdminService.createOrganization(
                new CreateOrganizationCommand("Hastane B")
        );

        BranchResponse branchA = organizationAdminService.createBranch(
                orgA.id(), new CreateBranchCommand("Şube A", "Europe/Istanbul", null)
        );
        ServiceDefinitionResponse serviceB = catalogManagementService.defineService(
                orgB.id(), new CreateServiceCommand("Hizmet B", null)
        );

        assertThatThrownBy(() -> catalogManagementService.assignServiceToBranch(
                branchA.id(),
                new AssignBranchServiceCommand(serviceB.id(), 30, new BigDecimal("1000.00"), "TRY")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cross-tenant violation");
    }

    @Test
    @DisplayName("Negative: should reject negative fee on price update and preserve previous price")
    void shouldRejectNegativeFeeUpdate() {
        OrganizationResponse org = organizationAdminService.createOrganization(
                new CreateOrganizationCommand("Hastane Fiyat Test")
        );
        BranchResponse branch = organizationAdminService.createBranch(
                org.id(), new CreateBranchCommand("Şube", "Europe/Istanbul", null)
        );
        ServiceDefinitionResponse service = catalogManagementService.defineService(
                org.id(), new CreateServiceCommand("Radyoloji", null)
        );

        BranchServiceResponse offering = catalogManagementService.assignServiceToBranch(
                branch.id(),
                new AssignBranchServiceCommand(service.id(), 20, new BigDecimal("800.00"), "TRY")
        );

        assertThatThrownBy(() -> catalogManagementService.updateServicePricing(
                offering.id(),
                new BigDecimal("-200.00")
        )).isInstanceOf(IllegalArgumentException.class);

        // Previous price must be preserved
        BranchServiceResponse current = catalogManagementService.getBranchService(offering.id());
        assertThat(current.feeAmount()).isEqualByComparingTo("800.00");
    }
}
