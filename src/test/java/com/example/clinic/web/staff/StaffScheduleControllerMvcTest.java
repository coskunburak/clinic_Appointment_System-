package com.example.clinic.web.staff;

import com.example.clinic.booking.application.AppointmentApprovalService;
import com.example.clinic.booking.application.AppointmentQueryService;
import com.example.clinic.booking.application.dto.AppointmentViewDto;
import com.example.clinic.booking.domain.AppointmentStatus;
import com.example.clinic.common.security.ClinicAuthenticationSuccessHandler;
import com.example.clinic.common.security.SecurityConfig;
import com.example.clinic.common.security.SecurityContextFacade;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Spring MVC slice tests for staff agenda and appointment lifecycle transitions (WP-S08-03, Section 13.3).
 */
@WebMvcTest(controllers = StaffScheduleController.class)
@Import({SecurityConfig.class, GlobalModelAttributesAdvice.class, WebExceptionHandler.class})
class StaffScheduleControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppointmentQueryService appointmentQueryService;

    @MockBean
    private AppointmentApprovalService appointmentApprovalService;

    @MockBean
    private SecurityContextFacade securityContextFacade;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @MockBean
    private ClinicAuthenticationSuccessHandler authenticationSuccessHandler;

    @Test
    @DisplayName("1. Staff can view daily schedule")
    void staffCanViewDailySchedule() throws Exception {
        UUID appId = UUID.randomUUID();
        AppointmentViewDto app = new AppointmentViewDto(
                appId, UUID.randomUUID(), "Ali Veli", UUID.randomUUID(), "Dr. Mehmet Öz", "Kardiyoloji",
                UUID.randomUUID(), "Merkez Şube", "Europe/Istanbul", UUID.randomUUID(), "Genel Muayene",
                Instant.now().plusSeconds(3600), Instant.now().plusSeconds(5400),
                LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(9, 30),
                "2026-11-20", "09:00 - 09:30", 30, BigDecimal.valueOf(500), "TRY",
                AppointmentStatus.PENDING, "badge bg-warning", "Beklemede", false, true, true, false, false
        );

        when(appointmentQueryService.getStaffAppointmentsForCurrentUser(any(LocalDate.class), eq("daily")))
                .thenReturn(List.of(app));

        mockMvc.perform(get("/staff/schedule")
                .with(user("doctor@clinic.test").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(view().name("staff/schedule"))
                .andExpect(model().attributeExists("appointments", "selectedDate", "view"))
                .andExpect(content().string(containsString("Ali Veli")))
                .andExpect(content().string(containsString("btn-approve-appointment")));
    }

    @Test
    @DisplayName("2. Staff can view weekly schedule")
    void staffCanViewWeeklySchedule() throws Exception {
        when(appointmentQueryService.getStaffAppointmentsForCurrentUser(any(LocalDate.class), eq("weekly")))
                .thenReturn(List.of());

        mockMvc.perform(get("/staff/schedule")
                .with(user("doctor@clinic.test").roles("STAFF"))
                .param("view", "weekly"))
                .andExpect(status().isOk())
                .andExpect(view().name("staff/schedule"))
                .andExpect(model().attribute("view", "weekly"));
    }

    @Test
    @DisplayName("4. Unauthorized schedule access is rejected")
    void unauthorizedScheduleAccessIsRejected() throws Exception {
        // Anonymous request redirects to login
        mockMvc.perform(get("/staff/schedule"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        // Patient user receives 403 Forbidden
        mockMvc.perform(get("/staff/schedule")
                .with(user("patient@clinic.test").roles("PATIENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("5. Approve POST invokes AppointmentApprovalService")
    void approvePostInvokesService() throws Exception {
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
    @DisplayName("6. Reject POST with valid reason invokes rejectAppointment")
    void rejectPostWithValidReasonInvokesService() throws Exception {
        UUID appointmentId = UUID.randomUUID();
        String reason = "Acil ameliyat nedeniyle randevu reddedildi.";

        mockMvc.perform(post("/staff/appointments/{id}/reject", appointmentId)
                .with(csrf())
                .with(user("doctor@clinic.test").roles("STAFF"))
                .param("reason", reason))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/schedule"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(appointmentApprovalService).rejectAppointment(appointmentId, reason);
    }

    @Test
    @DisplayName("6b. Reject POST with blank reason is rejected with error flash message")
    void rejectPostWithBlankReasonShowsErrorMessage() throws Exception {
        UUID appointmentId = UUID.randomUUID();

        mockMvc.perform(post("/staff/appointments/{id}/reject", appointmentId)
                .with(csrf())
                .with(user("doctor@clinic.test").roles("STAFF"))
                .param("reason", "   "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/schedule"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(appointmentApprovalService, never()).rejectAppointment(any(), any());
    }

    @Test
    @DisplayName("7. Complete POST invokes completeAppointment")
    void completePostInvokesService() throws Exception {
        UUID appointmentId = UUID.randomUUID();

        mockMvc.perform(post("/staff/appointments/{id}/complete", appointmentId)
                .with(csrf())
                .with(user("doctor@clinic.test").roles("STAFF")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/schedule"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(appointmentApprovalService).completeAppointment(appointmentId);
    }

    @Test
    @DisplayName("8. Invalid status transition produces safe error flash message")
    void invalidStatusTransitionProducesSafeErrorMessage() throws Exception {
        UUID appointmentId = UUID.randomUUID();
        doThrow(new IllegalStateException("Randevu bu durumda onaylanamaz."))
                .when(appointmentApprovalService).approveAppointment(appointmentId);

        mockMvc.perform(post("/staff/appointments/{id}/approve", appointmentId)
                .with(csrf())
                .with(user("doctor@clinic.test").roles("STAFF")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/schedule"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("10. CSRF-less POST is rejected with 403 Forbidden")
    void csrfLessPostIsRejected() throws Exception {
        UUID appointmentId = UUID.randomUUID();

        mockMvc.perform(post("/staff/appointments/{id}/approve", appointmentId)
                .with(user("doctor@clinic.test").roles("STAFF")))
                .andExpect(status().isForbidden());

        verify(appointmentApprovalService, never()).approveAppointment(any());
    }
}
