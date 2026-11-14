package com.example.clinic.web.admin;

import com.example.clinic.catalog.application.CatalogManagementService;
import com.example.clinic.catalog.application.dto.BranchServiceResponse;
import com.example.clinic.catalog.application.dto.ServiceDefinitionResponse;
import com.example.clinic.common.security.ClinicAuthenticationSuccessHandler;
import com.example.clinic.common.security.SecurityConfig;
import com.example.clinic.common.security.SecurityContextFacade;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import com.example.clinic.organization.application.OrganizationAdminService;
import com.example.clinic.organization.application.dto.BranchResponse;
import com.example.clinic.web.common.GlobalModelAttributesAdvice;
import com.example.clinic.web.error.WebExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = AdminServiceController.class)
@Import({SecurityConfig.class, GlobalModelAttributesAdvice.class, WebExceptionHandler.class})
class AdminServiceControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatalogManagementService catalogManagementService;

    @MockBean
    private OrganizationAdminService organizationAdminService;

    @MockBean
    private SecurityContextFacade securityContextFacade;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @MockBean
    private ClinicAuthenticationSuccessHandler authenticationSuccessHandler;

    @Test
    @DisplayName("1. Branch-scoped admin loads branch services without calling org-wide catalog queries")
    void branchAdminLoadsServicesWithoutOrgWideLeak() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(organizationAdminService.getOrgWideAuthorizedOrganizations()).thenReturn(List.of());
        when(organizationAdminService.getAuthorizedBranches()).thenReturn(List.of(
                new BranchResponse(branchId, orgId, "Şube 1", "Europe/Istanbul", null, true, Instant.now())
        ));
        when(catalogManagementService.getBranchServicesByBranch(branchId)).thenReturn(List.of(
                new BranchServiceResponse(UUID.randomUUID(), orgId, branchId, UUID.randomUUID(), "Kardiyoloji Muayenesi", 30, new BigDecimal("750.00"), "TRY", true, Instant.now())
        ));

        mockMvc.perform(get("/admin/services")
                        .with(user("branchadmin@clinic.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/services"))
                .andExpect(model().attribute("canManageOrgCatalog", false))
                .andExpect(content().string(containsString("Kardiyoloji Muayenesi")));

        verify(catalogManagementService, never()).getServiceDefinitionsByOrganization(any());
    }

    @Test
    @DisplayName("2. Branch service assignment form enforces price, duration, and currency constraints")
    void serviceFormsEnforceConstraints() throws Exception {
        mockMvc.perform(post("/admin/services/assign")
                        .with(csrf())
                        .with(user("admin@clinic.test").roles("ADMIN"))
                        .param("branchId", UUID.randomUUID().toString())
                        .param("serviceDefinitionId", UUID.randomUUID().toString())
                        .param("durationMinutes", "-10")
                        .param("fee", "-50.00")
                        .param("currency", "TR"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/services"))
                .andExpect(model().attributeHasFieldErrors("branchServiceForm", "durationMinutes", "fee", "currency"));

        verify(catalogManagementService, never()).assignServiceToBranch(any(), any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("3. Inline pricing and duration forms validate bad numeric input safely via redirect flash")
    void inlinePricingAndDurationFormsRejectInvalidValues() throws Exception {
        UUID bsId = UUID.randomUUID();

        mockMvc.perform(post("/admin/services/{id}/pricing", bsId)
                        .with(csrf())
                        .with(user("admin@clinic.test").roles("ADMIN"))
                        .param("fee", "-10.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/services"))
                .andExpect(flash().attributeExists("errorMessage"));

        mockMvc.perform(post("/admin/services/{id}/duration", bsId)
                        .with(csrf())
                        .with(user("admin@clinic.test").roles("ADMIN"))
                        .param("durationMinutes", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/services"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(catalogManagementService, never()).updateServicePricing(any(), any(BigDecimal.class));
        verify(catalogManagementService, never()).updateServiceDuration(any(), anyInt());
    }

    @Test
    @DisplayName("4. Service definition edit and lifecycle toggles delegate cleanly")
    void serviceDefinitionEditAndToggleDelegateCleanly() throws Exception {
        UUID defId = UUID.randomUUID();
        when(catalogManagementService.updateServiceDefinition(defId, "Güncel EKG", "Açıklama"))
                .thenReturn(new ServiceDefinitionResponse(defId, UUID.randomUUID(), "Güncel EKG", "Açıklama", true, Instant.now()));

        mockMvc.perform(post("/admin/services/definitions/{id}/edit", defId)
                        .with(csrf())
                        .with(user("admin@clinic.test").roles("ADMIN"))
                        .param("name", "Güncel EKG")
                        .param("description", "Açıklama"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/services"))
                .andExpect(flash().attributeExists("successMessage"));

        mockMvc.perform(post("/admin/services/definitions/{id}/deactivate", defId)
                        .with(csrf())
                        .with(user("admin@clinic.test").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/services"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(catalogManagementService).deactivateServiceDefinition(defId);
    }

    @Test
    @DisplayName("5. Cross-tenant service status update produces 403 Forbidden")
    void crossTenantServiceToggleProduces403() throws Exception {
        UUID foreignServiceId = UUID.randomUUID();
        doThrow(new AccessDeniedException("Forbidden"))
                .when(catalogManagementService).activateBranchService(foreignServiceId);

        mockMvc.perform(post("/admin/services/{id}/activate", foreignServiceId)
                        .with(csrf())
                        .with(user("admin@clinic.test").roles("ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"));
    }
}
