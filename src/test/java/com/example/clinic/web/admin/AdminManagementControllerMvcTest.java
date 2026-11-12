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
import com.example.clinic.organization.application.dto.CreateBranchCommand;
import com.example.clinic.organization.application.dto.DepartmentResponse;
import com.example.clinic.organization.application.dto.OpeningHoursDto;
import com.example.clinic.organization.application.dto.OrganizationResponse;
import com.example.clinic.staff.application.StaffManagementService;
import com.example.clinic.staff.application.dto.CreateStaffCommand;
import com.example.clinic.staff.application.dto.StaffProfileResponse;
import com.example.clinic.web.common.GlobalModelAttributesAdvice;
import com.example.clinic.web.error.WebExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Spring MVC slice tests for administrator management workflows (WP-S08-04, FR-028, Section 13.4).
 */
@WebMvcTest(controllers = AdminManagementController.class)
@Import({SecurityConfig.class, GlobalModelAttributesAdvice.class, WebExceptionHandler.class})
class AdminManagementControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrganizationAdminService organizationAdminService;

    @MockBean
    private CatalogManagementService catalogManagementService;

    @MockBean
    private StaffManagementService staffManagementService;

    @MockBean
    private SecurityContextFacade securityContextFacade;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @MockBean
    private ClinicAuthenticationSuccessHandler authenticationSuccessHandler;

    @Test
    @DisplayName("1. Authorized administrator opens branch management")
    void authorizedAdminOpensBranchManagement() throws Exception {
        UUID orgId = UUID.randomUUID();
        when(organizationAdminService.getAuthorizedOrganizations()).thenReturn(List.of(
                new OrganizationResponse(orgId, "Acıbadem Sağlık", true, Instant.now())
        ));
        when(organizationAdminService.getAuthorizedBranches()).thenReturn(List.of(
                new BranchResponse(UUID.randomUUID(), orgId, "Kadıköy Şubesi", "Europe/Istanbul", "İstanbul", true, Instant.now())
        ));

        mockMvc.perform(get("/admin/branches")
                .with(user("admin@clinic.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/branches"))
                .andExpect(model().attributeExists("organizations", "branches", "branchForm"))
                .andExpect(content().string(containsString("Kadıköy Şubesi")));
    }

    @Test
    @DisplayName("2. Administrator creates branch via application service")
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
    @DisplayName("3. Branch form validation rejects missing name")
    void branchFormValidationRejectsMissingName() throws Exception {
        mockMvc.perform(post("/admin/branches")
                .with(csrf())
                .with(user("admin@clinic.test").roles("ADMIN"))
                .param("organizationId", UUID.randomUUID().toString())
                .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/branches"))
                .andExpect(model().attributeHasFieldErrors("branchForm", "name"));

        verify(organizationAdminService, never()).createBranch(any(), any());
    }

    @Test
    @DisplayName("4. Working hours form validation works")
    void workingHoursFormValidationWorks() throws Exception {
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
    }

    @Test
    @DisplayName("5. Department management loads scoped data")
    void departmentManagementLoadsScopedData() throws Exception {
        UUID branchId = UUID.randomUUID();
        when(organizationAdminService.getAuthorizedBranches()).thenReturn(List.of(
                new BranchResponse(branchId, UUID.randomUUID(), "Merkez Şube", "Europe/Istanbul", null, true, Instant.now())
        ));
        when(organizationAdminService.getDepartmentsByBranch(branchId)).thenReturn(List.of(
                new DepartmentResponse(UUID.randomUUID(), branchId, "Kardiyoloji", true, Instant.now())
        ));

        mockMvc.perform(get("/admin/departments")
                .with(user("admin@clinic.test").roles("ADMIN"))
                .param("branchId", branchId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/departments"))
                .andExpect(model().attributeExists("departments", "branches"))
                .andExpect(content().string(containsString("Kardiyoloji")));
    }

    @Test
    @DisplayName("6. Service forms enforce price and duration constraints")
    void serviceFormsEnforceConstraints() throws Exception {
        mockMvc.perform(post("/admin/services/assign")
                .with(csrf())
                .with(user("admin@clinic.test").roles("ADMIN"))
                .param("branchId", UUID.randomUUID().toString())
                .param("serviceDefinitionId", UUID.randomUUID().toString())
                .param("durationMinutes", "-10") // Invalid negative duration
                .param("fee", "-50.00") // Invalid negative fee
                .param("currency", "TR")) // Invalid 2-char currency
                .andExpect(status().isOk());

        verify(catalogManagementService, never()).assignServiceToBranch(any(), any(), any(Integer.class), any(), any());
    }

    @Test
    @DisplayName("7. Staff management shows registered personnel and assignments")
    void staffManagementShowsPersonnel() throws Exception {
        UUID staffId = UUID.randomUUID();
        when(staffManagementService.getAllStaffProfiles()).thenReturn(List.of(
                new StaffProfileResponse(staffId, UUID.randomUUID(), "doktor@clinic.test", "Dr. Zeynep Kaya", "5551234", null, "Göz", true, Instant.now())
        ));

        mockMvc.perform(get("/admin/staff")
                .with(user("admin@clinic.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/staff"))
                .andExpect(model().attributeExists("staffProfiles", "branches", "departments"))
                .andExpect(content().string(containsString("Dr. Zeynep Kaya")));
    }

    @Test
    @DisplayName("9. Patient access to admin pages fails with 403 Forbidden")
    void patientAccessToAdminPagesFails() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                .with(user("patient@clinic.test").roles("PATIENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("10. Staff access to admin pages fails with 403 Forbidden")
    void staffAccessToAdminPagesFails() throws Exception {
        mockMvc.perform(get("/admin/branches")
                .with(user("doctor@clinic.test").roles("STAFF")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("11. CSRF-less admin mutation is rejected with 403 Forbidden")
    void csrfLessAdminMutationFails() throws Exception {
        mockMvc.perform(post("/admin/branches")
                .with(user("admin@clinic.test").roles("ADMIN"))
                .param("organizationId", UUID.randomUUID().toString())
                .param("name", "Kadıköy Şubesi"))
                .andExpect(status().isForbidden());

        verify(organizationAdminService, never()).createBranch(any(), any());
    }

    @Test
    @DisplayName("12. Successful admin mutation uses Post/Redirect/Get pattern")
    void successfulAdminMutationUsesPostRedirectGet() throws Exception {
        mockMvc.perform(post("/admin/departments")
                .with(csrf())
                .with(user("admin@clinic.test").roles("ADMIN"))
                .param("branchId", UUID.randomUUID().toString())
                .param("name", "Radyoloji"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/departments*"))
                .andExpect(flash().attributeExists("successMessage"));
    }
}
