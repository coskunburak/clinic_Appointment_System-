package com.example.clinic.web.admin;

import com.example.clinic.catalog.application.CatalogManagementService;
import com.example.clinic.catalog.application.dto.BranchServiceResponse;
import com.example.clinic.common.security.ClinicAuthenticationSuccessHandler;
import com.example.clinic.common.security.SecurityConfig;
import com.example.clinic.common.security.SecurityContextFacade;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import com.example.clinic.organization.application.OrganizationAdminService;
import com.example.clinic.organization.application.dto.BranchResponse;
import com.example.clinic.organization.application.dto.DepartmentResponse;
import com.example.clinic.organization.application.dto.OrganizationResponse;
import com.example.clinic.staff.application.StaffManagementService;
import com.example.clinic.staff.application.dto.StaffProfileResponse;
import com.example.clinic.web.common.GlobalModelAttributesAdvice;
import com.example.clinic.web.error.WebExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = AdminDashboardController.class)
@Import({SecurityConfig.class, GlobalModelAttributesAdvice.class, WebExceptionHandler.class})
class AdminDashboardControllerMvcTest {

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
    @DisplayName("Administrator views scoped organization, branch, department, service, and staff counts")
    void adminViewsScopedDashboardCounts() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(organizationAdminService.getAuthorizedOrganizations()).thenReturn(List.of(
                new OrganizationResponse(orgId, "Anadolu Kliniği", true, Instant.now())
        ));
        when(organizationAdminService.getOrgWideAuthorizedOrganizations()).thenReturn(List.of(
                new OrganizationResponse(orgId, "Anadolu Kliniği", true, Instant.now())
        ));
        when(organizationAdminService.getAuthorizedBranches()).thenReturn(List.of(
                new BranchResponse(branchId, orgId, "Kadıköy Şubesi", "Europe/Istanbul", "Kadıköy", true, Instant.now())
        ));
        when(organizationAdminService.getDepartmentsByBranch(branchId)).thenReturn(List.of(
                new DepartmentResponse(UUID.randomUUID(), branchId, "Kardiyoloji", true, Instant.now())
        ));
        when(catalogManagementService.getBranchServicesByBranch(branchId)).thenReturn(List.of(
                new BranchServiceResponse(UUID.randomUUID(), orgId, branchId, UUID.randomUUID(), "Muayene", 30, new BigDecimal("500.00"), "TRY", true, Instant.now())
        ));
        when(staffManagementService.getAuthorizedStaffProfiles()).thenReturn(List.of(
                new StaffProfileResponse(UUID.randomUUID(), UUID.randomUUID(), "dr@clinic.test", "Dr. Ali", "555", "DOCTOR", "Kardiyoloji", true, Instant.now())
        ));

        mockMvc.perform(get("/admin/dashboard")
                        .with(user("admin@clinic.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attribute("organizationCount", 1))
                .andExpect(model().attribute("branchCount", 1))
                .andExpect(model().attribute("departmentCount", 1))
                .andExpect(model().attribute("serviceCount", 1))
                .andExpect(model().attribute("staffCount", 1))
                .andExpect(content().string(containsString("Yönetici Paneli")));

        verify(staffManagementService).getAuthorizedStaffProfiles();
    }

    @Test
    @DisplayName("Patient access to /admin/dashboard fails with 403 Forbidden")
    void patientAccessFailsWith403() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .with(user("patient@clinic.test").roles("PATIENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Staff access to /admin/dashboard fails with 403 Forbidden")
    void staffAccessFailsWith403() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .with(user("doctor@clinic.test").roles("STAFF")))
                .andExpect(status().isForbidden());
    }
}
