package com.example.clinic.web;

import com.example.clinic.booking.application.AppointmentApprovalService;
import com.example.clinic.booking.application.AppointmentQueryService;
import com.example.clinic.booking.application.BookingService;
import com.example.clinic.booking.application.PatientBookingQueryService;
import com.example.clinic.booking.application.dto.BranchSummaryDto;
import com.example.clinic.catalog.application.CatalogManagementService;
import com.example.clinic.common.security.ClinicAuthenticationSuccessHandler;
import com.example.clinic.common.security.SecurityConfig;
import com.example.clinic.common.security.SecurityContextFacade;
import com.example.clinic.identity.infrastructure.PatientProfileRepository;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import com.example.clinic.organization.application.OrganizationAdminService;
import com.example.clinic.staff.application.StaffManagementService;
import com.example.clinic.web.admin.AdminBranchController;
import com.example.clinic.web.admin.AdminDashboardController;
import com.example.clinic.web.admin.AdminDepartmentController;
import com.example.clinic.web.admin.AdminServiceController;
import com.example.clinic.web.admin.AdminStaffController;
import com.example.clinic.web.common.GlobalModelAttributesAdvice;
import com.example.clinic.web.error.CustomErrorController;
import com.example.clinic.web.error.WebExceptionHandler;
import com.example.clinic.web.patient.PatientBookingController;
import com.example.clinic.web.staff.StaffScheduleController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
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
 * Sprint S08 Master Acceptance Specification Test Suite (S08.md, Section 3, TC-027, TC-028, TC-045).
 * Contains the 5 mandatory specification test methods verifying role segregation,
 * server-side form validation, staff approval POST flow, authentication redirects, and custom 403 error handling.
 */
@WebMvcTest(controllers = {
        PatientBookingController.class,
        StaffScheduleController.class,
        AdminDashboardController.class,
        AdminBranchController.class,
        AdminDepartmentController.class,
        AdminServiceController.class,
        AdminStaffController.class,
        CustomErrorController.class
})
@Import({SecurityConfig.class, GlobalModelAttributesAdvice.class, WebExceptionHandler.class})
class Sprint08AcceptanceSpecificationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private AppointmentApprovalService appointmentApprovalService;

    @MockBean
    private AppointmentQueryService appointmentQueryService;

    @MockBean
    private PatientBookingQueryService patientBookingQueryService;

    @MockBean
    private PatientProfileRepository patientProfileRepository;

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
    @DisplayName("TC-027: patientCanViewBookingPage — Authenticated patient opens /patient/book and sees HTML form elements")
    void patientCanViewBookingPage() throws Exception {
        UUID branchId = UUID.randomUUID();
        when(patientBookingQueryService.getActiveBranches()).thenReturn(List.of(
                new BranchSummaryDto(branchId, "Kadıköy Şubesi", "Europe/Istanbul", "İstanbul")
        ));

        mockMvc.perform(get("/patient/book")
                .with(user("patient@clinic.test").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/book"))
                .andExpect(model().attributeExists("branches", "bookingForm"))
                .andExpect(content().string(containsString("booking-form")))
                .andExpect(content().string(containsString("Kadıköy Şubesi")));
    }

    @Test
    @DisplayName("TC-027: invalidBookingFormShowsValidationErrors — Empty form submission re-renders view with validation errors")
    void invalidBookingFormShowsValidationErrors() throws Exception {
        mockMvc.perform(post("/patient/book")
                .with(csrf())
                .with(user("patient@clinic.test").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/book"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("bookingForm", "branchId", "branchServiceId", "staffId", "appointmentDate", "startTime"));
    }

    @Test
    @DisplayName("TC-027: staffCanApproveAppointmentViaPost — Staff POST to approve invokes service and redirects with flash message")
    void staffCanApproveAppointmentViaPost() throws Exception {
        UUID appointmentId = UUID.randomUUID();

        mockMvc.perform(post("/staff/appointments/{id}/approve", appointmentId)
                .with(csrf())
                .with(user("doctor@clinic.test").roles("STAFF")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/schedule"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(appointmentApprovalService).approveAppointment(appointmentId);
    }

    @Test
    @DisplayName("TC-037: unauthorizedAccessRedirectsToLogin — Unauthenticated request to /patient/appointments redirects to login")
    void unauthorizedAccessRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/patient/appointments"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("TC-045: customErrorPageHandles403 — Patient requesting /admin/** is denied with 403 Forbidden")
    void customErrorPageHandles403() throws Exception {
        mockMvc.perform(get("/admin/services")
                .with(user("patient@clinic.test").roles("PATIENT")))
                .andExpect(status().isForbidden());
    }
}
