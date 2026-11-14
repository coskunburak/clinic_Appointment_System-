package com.example.clinic.web.admin;

import com.example.clinic.catalog.application.CatalogManagementService;
import com.example.clinic.common.security.ClinicAuthenticationSuccessHandler;
import com.example.clinic.common.security.SecurityConfig;
import com.example.clinic.common.security.SecurityContextFacade;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import com.example.clinic.organization.application.OrganizationAdminService;
import com.example.clinic.organization.application.dto.BranchResponse;
import com.example.clinic.staff.application.StaffManagementService;
import com.example.clinic.staff.application.dto.AvailabilityExceptionResponse;
import com.example.clinic.staff.application.dto.CreateStaffCommand;
import com.example.clinic.staff.application.dto.StaffAssignmentResponse;
import com.example.clinic.staff.application.dto.StaffProfileResponse;
import com.example.clinic.staff.application.dto.WeeklyAvailabilityResponse;
import com.example.clinic.staff.domain.AvailabilityExceptionKind;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = AdminStaffController.class)
@Import({SecurityConfig.class, GlobalModelAttributesAdvice.class, WebExceptionHandler.class})
class AdminStaffControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StaffManagementService staffManagementService;

    @MockBean
    private OrganizationAdminService organizationAdminService;

    @MockBean
    private CatalogManagementService catalogManagementService;

    @MockBean
    private SecurityContextFacade securityContextFacade;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @MockBean
    private ClinicAuthenticationSuccessHandler authenticationSuccessHandler;

    @Test
    @DisplayName("1. Staff management uses tenant-scoped queries instead of global staff list")
    void staffManagementShowsScopedPersonnel() throws Exception {
        UUID staffId = UUID.randomUUID();
        when(staffManagementService.getAuthorizedStaffProfiles()).thenReturn(List.of(
                new StaffProfileResponse(staffId, UUID.randomUUID(), "doktor@clinic.test", "Dr. Zeynep Kaya", "5551234", "DOCTOR", "Göz", true, Instant.now())
        ));
        when(staffManagementService.getAuthorizedStaffAssignments()).thenReturn(List.of());

        mockMvc.perform(get("/admin/staff")
                        .with(user("admin@clinic.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/staff"))
                .andExpect(model().attributeExists("staffProfiles", "branches", "departments"))
                .andExpect(content().string(containsString("Dr. Zeynep Kaya")));

        verify(staffManagementService, never()).getAllStaffProfiles();
    }

    @Test
    @DisplayName("2. Registering DOCTOR without specialty fails validation cleanly")
    void doctorRegistrationRequiresSpecialty() throws Exception {
        mockMvc.perform(post("/admin/staff")
                        .with(csrf())
                        .with(user("admin@clinic.test").roles("ADMIN"))
                        .param("branchId", UUID.randomUUID().toString())
                        .param("fullName", "Dr. Demir")
                        .param("email", "demir@clinic.test")
                        .param("password", "Secret123!")
                        .param("staffType", "DOCTOR")
                        .param("specialty", "   "))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/staff"))
                .andExpect(model().attributeHasFieldErrors("staffForm", "specialty"));

        verify(staffManagementService, never()).registerStaff(any());
    }

    @Test
    @DisplayName("3. Valid DOCTOR registration resolves organization from branch and creates staff")
    void validDoctorRegistrationWorks() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(organizationAdminService.getBranch(branchId)).thenReturn(
                new BranchResponse(branchId, orgId, "Merkez", "Europe/Istanbul", "Adres", true, Instant.now())
        );

        mockMvc.perform(post("/admin/staff")
                        .with(csrf())
                        .with(user("admin@clinic.test").roles("ADMIN"))
                        .param("branchId", branchId.toString())
                        .param("fullName", "Dr. Demir")
                        .param("email", "demir@clinic.test")
                        .param("password", "Secret123!")
                        .param("staffType", "DOCTOR")
                        .param("specialty", "Dahiliye"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/staff"))
                .andExpect(flash().attributeExists("successMessage"));

        ArgumentCaptor<CreateStaffCommand> captor = ArgumentCaptor.forClass(CreateStaffCommand.class);
        verify(staffManagementService).registerStaff(captor.capture());
        assertThat(captor.getValue().organizationId()).isEqualTo(orgId);
        assertThat(captor.getValue().specialty()).isEqualTo("Dahiliye");
    }

    @Test
    @DisplayName("4. Availability page supports selected assignmentId and partial-leave validation")
    void availabilitySelectionAndExceptionValidationWork() throws Exception {
        UUID staffId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();

        when(staffManagementService.getAuthorizedStaffProfile(staffId)).thenReturn(
                new StaffProfileResponse(staffId, UUID.randomUUID(), "dr@clinic.test", "Dr. Ayşe", null, "DOCTOR", "Nöroloji", true, Instant.now())
        );
        when(staffManagementService.getAuthorizedStaffAssignmentsByStaff(staffId)).thenReturn(List.of(
                new StaffAssignmentResponse(assignmentId, staffId, "Dr. Ayşe", "DOCTOR", "Nöroloji", UUID.randomUUID(), "Merkez", UUID.randomUUID(), "Nöroloji", true, Instant.now())
        ));
        when(staffManagementService.getAuthorizedWeeklyAvailabilitiesByAssignment(assignmentId)).thenReturn(List.of(
                new WeeklyAvailabilityResponse(UUID.randomUUID(), assignmentId, (short) 1, LocalTime.of(9, 0), LocalTime.of(17, 0))
        ));
        when(staffManagementService.getAuthorizedAvailabilityExceptionsByAssignment(assignmentId)).thenReturn(List.of(
                new AvailabilityExceptionResponse(UUID.randomUUID(), assignmentId, LocalDate.of(2026, 12, 1), AvailabilityExceptionKind.FULL_DAY_LEAVE, null, null, "İzin", Instant.now())
        ));

        mockMvc.perform(get("/admin/staff/{id}/availability", staffId)
                        .param("assignmentId", assignmentId.toString())
                        .with(user("admin@clinic.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/staff-availability"))
                .andExpect(model().attribute("selectedAssignmentId", assignmentId));

        // PARTIAL_LEAVE missing times is rejected before calling service
        mockMvc.perform(post("/admin/staff/assignments/{assignmentId}/exceptions", assignmentId)
                        .with(csrf())
                        .with(user("admin@clinic.test").roles("ADMIN"))
                        .param("staffId", staffId.toString())
                        .param("assignmentId", assignmentId.toString())
                        .param("localDate", "2026-12-05")
                        .param("kind", "PARTIAL_LEAVE"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/staff-availability"))
                .andExpect(model().attributeHasFieldErrors("exceptionForm", "startTime", "endTime"));

        verify(staffManagementService, never()).addAvailabilityException(any());
    }

    @Test
    @DisplayName("5. Deleting weekly availability and exception redirects cleanly back to availability page")
    void deleteWeeklyAndExceptionWorks() throws Exception {
        UUID staffId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID availabilityId = UUID.randomUUID();
        UUID exceptionId = UUID.randomUUID();

        mockMvc.perform(post("/admin/staff/assignments/{assignmentId}/weekly-availability/{availabilityId}/delete", assignmentId, availabilityId)
                        .with(csrf())
                        .with(user("admin@clinic.test").roles("ADMIN"))
                        .param("staffId", staffId.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/staff/" + staffId + "/availability?assignmentId=" + assignmentId))
                .andExpect(flash().attributeExists("successMessage"));

        verify(staffManagementService).deleteWeeklyAvailability(assignmentId, availabilityId);

        mockMvc.perform(post("/admin/staff/assignments/{assignmentId}/exceptions/{exceptionId}/delete", assignmentId, exceptionId)
                        .with(csrf())
                        .with(user("admin@clinic.test").roles("ADMIN"))
                        .param("staffId", staffId.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/staff/" + staffId + "/availability?assignmentId=" + assignmentId))
                .andExpect(flash().attributeExists("successMessage"));

        verify(staffManagementService).deleteAvailabilityException(assignmentId, exceptionId);
    }

    @Test
    @DisplayName("6. Cross-tenant staff profile access returns 403 and invalid UUID returns 400")
    void crossTenantAccessReturns403AndInvalidUuidReturns400() throws Exception {
        UUID foreignStaffId = UUID.randomUUID();
        when(staffManagementService.getAuthorizedStaffProfile(foreignStaffId))
                .thenThrow(new AccessDeniedException("Forbidden"));

        mockMvc.perform(get("/admin/staff/{id}/availability", foreignStaffId)
                        .with(user("admin@clinic.test").roles("ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"));

        mockMvc.perform(get("/admin/staff/not-a-valid-uuid/availability")
                        .with(user("admin@clinic.test").roles("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/400"));
    }
}
