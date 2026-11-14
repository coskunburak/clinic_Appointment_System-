package com.example.clinic.web.admin;

import com.example.clinic.common.security.ClinicAuthenticationSuccessHandler;
import com.example.clinic.common.security.SecurityConfig;
import com.example.clinic.common.security.SecurityContextFacade;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import com.example.clinic.organization.application.OrganizationAdminService;
import com.example.clinic.organization.application.dto.BranchResponse;
import com.example.clinic.organization.application.dto.DepartmentResponse;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
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

@WebMvcTest(controllers = AdminDepartmentController.class)
@Import({SecurityConfig.class, GlobalModelAttributesAdvice.class, WebExceptionHandler.class})
class AdminDepartmentControllerMvcTest {

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
    @DisplayName("1. Department management loads scoped data")
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
                .andExpect(model().attributeExists("departments", "branches", "selectedBranchId"))
                .andExpect(content().string(containsString("Kardiyoloji")));
    }

    @Test
    @DisplayName("2. Creating department redirects using Post/Redirect/Get with selected branchId")
    void successfulDepartmentCreationUsesPostRedirectGet() throws Exception {
        UUID branchId = UUID.randomUUID();

        mockMvc.perform(post("/admin/departments")
                        .with(csrf())
                        .with(user("admin@clinic.test").roles("ADMIN"))
                        .param("branchId", branchId.toString())
                        .param("name", "Radyoloji"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/departments?branchId=" + branchId))
                .andExpect(flash().attributeExists("successMessage"));

        verify(organizationAdminService).createDepartment(branchId, "Radyoloji");
    }

    @Test
    @DisplayName("3. Department edit and status toggle invoke OrganizationAdminService and keep branch context")
    void departmentEditAndToggleWork() throws Exception {
        UUID branchId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();
        when(organizationAdminService.updateDepartment(deptId, "İç Hastalıkları")).thenReturn(
                new DepartmentResponse(deptId, branchId, "İç Hastalıkları", true, Instant.now())
        );

        mockMvc.perform(post("/admin/departments/{id}/edit", deptId)
                        .with(csrf())
                        .with(user("admin@clinic.test").roles("ADMIN"))
                        .param("branchId", branchId.toString())
                        .param("name", "İç Hastalıkları"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/departments?branchId=" + branchId))
                .andExpect(flash().attributeExists("successMessage"));

        mockMvc.perform(post("/admin/departments/{id}/deactivate", deptId)
                        .with(csrf())
                        .with(user("admin@clinic.test").roles("ADMIN"))
                        .param("branchId", branchId.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/departments*"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(organizationAdminService).deactivateDepartment(deptId);
    }

    @Test
    @DisplayName("4. Cross-tenant branchId query triggers 403 Forbidden instead of empty list")
    void crossTenantDepartmentQueryReturns403() throws Exception {
        UUID foreignBranchId = UUID.randomUUID();
        when(organizationAdminService.getDepartmentsByBranch(foreignBranchId))
                .thenThrow(new AccessDeniedException("Forbidden"));

        mockMvc.perform(get("/admin/departments")
                        .with(user("admin@clinic.test").roles("ADMIN"))
                        .param("branchId", foreignBranchId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"));
    }

    @Test
    @DisplayName("5. Blank department name is rejected by Jakarta Validation")
    void blankDepartmentNameIsRejected() throws Exception {
        UUID branchId = UUID.randomUUID();

        mockMvc.perform(post("/admin/departments")
                        .with(csrf())
                        .with(user("admin@clinic.test").roles("ADMIN"))
                        .param("branchId", branchId.toString())
                        .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/departments"))
                .andExpect(model().attributeHasFieldErrors("departmentForm", "name"));

        verify(organizationAdminService, never()).createDepartment(any(), any());
    }
}
