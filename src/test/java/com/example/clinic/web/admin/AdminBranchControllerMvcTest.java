package com.example.clinic.web.admin;

import com.example.clinic.common.security.ClinicAuthenticationSuccessHandler;
import com.example.clinic.common.security.SecurityConfig;
import com.example.clinic.common.security.SecurityContextFacade;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import com.example.clinic.organization.application.OrganizationAdminService;
import com.example.clinic.organization.application.dto.BranchResponse;
import com.example.clinic.organization.application.dto.CreateBranchCommand;
import com.example.clinic.organization.application.dto.OpeningHoursDto;
import com.example.clinic.organization.application.dto.OrganizationResponse;
import com.example.clinic.web.common.GlobalModelAttributesAdvice;
import com.example.clinic.web.error.WebExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

@WebMvcTest(controllers = AdminBranchController.class)
@Import({SecurityConfig.class, GlobalModelAttributesAdvice.class, WebExceptionHandler.class})
class AdminBranchControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrganizationAdminService organizationAdminService;

    @MockBean
    private SecurityContextFacade securityContextFacade;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @MockBean
    private ClinicAuthenticationSuccessHandler authenticationSuccessHandler;

    @Test
    @DisplayName("1. Administrator views authorized organizations and branches")
    void adminViewsBranchesPage() throws Exception {
        UUID orgId = UUID.randomUUID();
        when(organizationAdminService.getAuthorizedOrganizations()).thenReturn(List.of(
                new OrganizationResponse(orgId, "Klinik A", true, Instant.now())
        ));
        when(organizationAdminService.getOrgWideAuthorizedOrganizations()).thenReturn(List.of(
                new OrganizationResponse(orgId, "Klinik A", true, Instant.now())
        ));
        when(organizationAdminService.getAuthorizedBranches()).thenReturn(List.of(
                new BranchResponse(UUID.randomUUID(), orgId, "Kadıköy Şubesi", "Europe/Istanbul", "Kadıköy", true, Instant.now())
        ));

        mockMvc.perform(get("/admin/branches")
                        .with(user("admin@clinic.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/branches"))
                .andExpect(model().attributeExists("organizations", "orgWideOrganizations", "orgWideOrgIds", "branches", "branchForm"))
                .andExpect(content().string(containsString("Kadıköy Şubesi")));
    }

    @Test
    @DisplayName("2. Administrator creates branch via application service using PRG")
    void adminCreatesBranchViaService() throws Exception {
        UUID orgId = UUID.randomUUID();

        mockMvc.perform(post("/admin/branches")
                        .with(csrf())
                        .with(user("admin@clinic.test").roles("ADMIN"))
                        .param("organizationId", orgId.toString())
                        .param("name", "Beşiktaş Şubesi")
                        .param("timeZone", "Europe/Istanbul")
                        .param("address", "Beşiktaş, İstanbul"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/branches"))
                .andExpect(flash().attributeExists("successMessage"));

        ArgumentCaptor<CreateBranchCommand> captor = ArgumentCaptor.forClass(CreateBranchCommand.class);
        verify(organizationAdminService).createBranch(eq(orgId), captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Beşiktaş Şubesi");
    }

    @Test
    @DisplayName("3. Branch form validation rejects missing name and invalid timezone")
    void branchFormValidationRejectsMissingNameAndInvalidTimeZone() throws Exception {
        mockMvc.perform(post("/admin/branches")
                        .with(csrf())
                        .with(user("admin@clinic.test").roles("ADMIN"))
                        .param("organizationId", UUID.randomUUID().toString())
                        .param("name", "")
                        .param("timeZone", "Mars/Olympus"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/branches"))
                .andExpect(model().attributeHasFieldErrors("branchForm", "name", "timeZone"));

        verify(organizationAdminService, never()).createBranch(any(), any());
    }

    @Test
    @DisplayName("4. Administrator can update organization name and branch details")
    void adminCanUpdateOrganizationAndBranch() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        mockMvc.perform(post("/admin/organizations/{id}/edit", orgId)
                        .with(csrf())
                        .with(user("admin@clinic.test").roles("ADMIN"))
                        .param("name", "Yeni Anadolu Kliniği"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/branches"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(organizationAdminService).updateOrganization(orgId, "Yeni Anadolu Kliniği");

        mockMvc.perform(post("/admin/branches/{id}/edit", branchId)
                        .with(csrf())
                        .with(user("admin@clinic.test").roles("ADMIN"))
                        .param("name", "Ataşehir Şubesi")
                        .param("timeZone", "Europe/Istanbul")
                        .param("address", "Ataşehir"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/branches"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(organizationAdminService).updateBranch(branchId, "Ataşehir Şubesi", "Europe/Istanbul", "Ataşehir");
    }

    @Test
    @DisplayName("5. Cross-tenant branch edit throws AccessDeniedException and returns 403 Forbidden")
    void crossTenantBranchEditReturns403() throws Exception {
        UUID foreignBranchId = UUID.randomUUID();
        doThrow(new AccessDeniedException("Forbidden"))
                .when(organizationAdminService).updateBranch(eq(foreignBranchId), any(), any(), any());

        mockMvc.perform(post("/admin/branches/{id}/edit", foreignBranchId)
                        .with(csrf())
                        .with(user("admin@clinic.test").roles("ADMIN"))
                        .param("name", "Yabancı Şube")
                        .param("timeZone", "Europe/Istanbul"))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"));
    }

    @Test
    @DisplayName("6. Working hours form validation, save, and weekday removal work cleanly")
    void workingHoursManagementWorks() throws Exception {
        UUID branchId = UUID.randomUUID();
        when(organizationAdminService.getBranch(branchId)).thenReturn(
                new BranchResponse(branchId, UUID.randomUUID(), "Merkez", "Europe/Istanbul", "Adres", true, Instant.now())
        );

        mockMvc.perform(post("/admin/branches/{id}/hours", branchId)
                        .with(csrf())
                        .with(user("admin@clinic.test").roles("ADMIN"))
                        .param("branchId", branchId.toString())
                        .param("weekday", "1")
                        .param("startTime", "09:00")
                        .param("endTime", "17:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/branches/" + branchId + "/hours"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(organizationAdminService).configureOpeningHours(eq(branchId), any());

        // Delete Tuesday hour while preserving Monday
        when(organizationAdminService.getOpeningHoursByBranch(branchId)).thenReturn(List.of(
                new OpeningHoursDto((short) 1, LocalTime.of(9, 0), LocalTime.of(17, 0)),
                new OpeningHoursDto((short) 2, LocalTime.of(9, 0), LocalTime.of(17, 0))
        ));

        mockMvc.perform(post("/admin/branches/{id}/hours/{weekday}/delete", branchId, 2)
                        .with(csrf())
                        .with(user("admin@clinic.test").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/branches/" + branchId + "/hours"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("7. Staff access and CSRF-less POST on branch routes fail with 403 Forbidden")
    void authorizationAndCsrfEnforced() throws Exception {
        mockMvc.perform(get("/admin/branches")
                        .with(user("doctor@clinic.test").roles("STAFF")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/branches")
                        .with(user("admin@clinic.test").roles("ADMIN"))
                        .param("organizationId", UUID.randomUUID().toString())
                        .param("name", "Kadıköy Şubesi"))
                .andExpect(status().isForbidden());

        verify(organizationAdminService, never()).createBranch(any(), any());
    }

    @Test
    @DisplayName("8. GET /admin/hours renders empty state safely when no branches exist")
    void defaultHoursEmptyStateWorks() throws Exception {
        when(organizationAdminService.getAuthorizedBranches()).thenReturn(List.of());

        mockMvc.perform(get("/admin/hours")
                        .with(user("admin@clinic.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/branch-hours"))
                .andExpect(model().attributeExists("branches", "openingHours", "hoursForm"));
    }
}
