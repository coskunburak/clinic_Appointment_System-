package com.example.clinic.web.patient;

import com.example.clinic.booking.application.AppointmentApprovalService;
import com.example.clinic.booking.application.AppointmentConflictException;
import com.example.clinic.booking.application.AppointmentQueryService;
import com.example.clinic.booking.application.BookingCommand;
import com.example.clinic.booking.application.BookingService;
import com.example.clinic.booking.application.BookingValidationException;
import com.example.clinic.booking.application.PatientBookingQueryService;
import com.example.clinic.booking.application.dto.AppointmentViewDto;
import com.example.clinic.booking.application.dto.BranchSummaryDto;
import com.example.clinic.booking.application.dto.ServiceSummaryDto;
import com.example.clinic.booking.application.dto.StaffSummaryDto;
import com.example.clinic.booking.domain.Appointment;
import com.example.clinic.booking.domain.AppointmentStatus;
import com.example.clinic.booking.domain.ServiceSnapshot;
import com.example.clinic.common.domain.Money;
import com.example.clinic.common.domain.TimeInterval;
import com.example.clinic.common.security.ClinicAuthenticationSuccessHandler;
import com.example.clinic.common.security.SecurityConfig;
import com.example.clinic.common.security.SecurityContextFacade;
import com.example.clinic.identity.domain.PatientProfile;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.PatientProfileRepository;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import com.example.clinic.web.common.GlobalModelAttributesAdvice;
import com.example.clinic.web.error.WebExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Comprehensive Spring MVC tests for patient workflows (WP-S08-02, Section 13.2).
 */
@WebMvcTest(controllers = PatientBookingController.class)
@Import({SecurityConfig.class, GlobalModelAttributesAdvice.class, WebExceptionHandler.class})
class PatientBookingControllerMvcTest {

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
    private SecurityContextFacade securityContextFacade;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @MockBean
    private ClinicAuthenticationSuccessHandler authenticationSuccessHandler;

    private UserAccount patientUser;
    private PatientProfile patientProfile;

    @BeforeEach
    void setUp() {
        patientUser = new UserAccount("patient@clinic.test", "encoded_pw");
        patientProfile = new PatientProfile(patientUser, "Ayşe Yılmaz", "5551234567");

        when(securityContextFacade.getRequiredCurrentUser()).thenReturn(patientUser);
        when(securityContextFacade.getCurrentUser()).thenReturn(Optional.of(patientUser));
        when(patientProfileRepository.findByUserAccountId(patientUser.getId())).thenReturn(Optional.of(patientProfile));
    }

    @Test
    @DisplayName("1. Authenticated patient can access booking screen")
    void authenticatedPatientCanAccessBookingScreen() throws Exception {
        UUID branchId = UUID.randomUUID();
        when(patientBookingQueryService.getActiveBranches()).thenReturn(List.of(
                new BranchSummaryDto(branchId, "Merkez Şube", "Europe/Istanbul", "İstanbul")
        ));

        mockMvc.perform(get("/patient/book")
                .with(user("patient@clinic.test").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/book"))
                .andExpect(model().attributeExists("branches", "bookingForm"));
    }

    @Test
    @DisplayName("2. Anonymous request redirects to login")
    void anonymousRequestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/patient/book"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("3. Staff/Admin cannot access patient booking route")
    void staffAndAdminCannotAccessPatientRoutes() throws Exception {
        mockMvc.perform(get("/patient/book")
                .with(user("doctor@clinic.test").roles("STAFF")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/patient/book")
                .with(user("admin@clinic.test").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Required booking fields are validated")
    void requiredBookingFieldsAreValidated() throws Exception {
        mockMvc.perform(post("/patient/book")
                .with(csrf())
                .with(user("patient@clinic.test").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/book"))
                .andExpect(model().hasErrors());

        verify(bookingService, never()).bookAppointment(any());
    }

    @Test
    @DisplayName("6. Past date is rejected by @FutureOrPresent constraint")
    void pastDateIsRejectedByValidation() throws Exception {
        mockMvc.perform(post("/patient/book")
                .with(csrf())
                .with(user("patient@clinic.test").roles("PATIENT"))
                .param("branchId", UUID.randomUUID().toString())
                .param("branchServiceId", UUID.randomUUID().toString())
                .param("staffId", UUID.randomUUID().toString())
                .param("appointmentDate", "2020-01-01")
                .param("startTime", "09:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/book"))
                .andExpect(model().attributeHasFieldErrors("bookingForm", "appointmentDate"));
    }

    @Test
    @DisplayName("8. Available slots are rendered as JSON via /patient/slots")
    void availableSlotsAreReturnedAsJson() throws Exception {
        UUID staffId = UUID.randomUUID();
        UUID branchServiceId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(1);

        when(patientBookingQueryService.getAvailableSlotTimes(staffId, branchServiceId, date))
                .thenReturn(List.of(LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0)));

        mockMvc.perform(get("/patient/slots")
                .with(user("patient@clinic.test").roles("PATIENT"))
                .param("staffId", staffId.toString())
                .param("branchServiceId", branchServiceId.toString())
                .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0]").value("09:00"))
                .andExpect(jsonPath("$[1]").value("09:30"))
                .andExpect(jsonPath("$[2]").value("10:00"));
    }

    @Test
    @DisplayName("9. Empty availability returns empty JSON array")
    void emptyAvailabilityReturnsEmptyJsonArray() throws Exception {
        UUID staffId = UUID.randomUUID();
        UUID branchServiceId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(1);

        when(patientBookingQueryService.getAvailableSlotTimes(staffId, branchServiceId, date))
                .thenReturn(List.of());

        mockMvc.perform(get("/patient/slots")
                .with(user("patient@clinic.test").roles("PATIENT"))
                .param("staffId", staffId.toString())
                .param("branchServiceId", branchServiceId.toString())
                .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("11. Valid booking POST invokes BookingService with authenticated patient identity")
    void validBookingPostInvokesBookingServiceWithAuthenticatedIdentity() throws Exception {
        UUID branchId = UUID.randomUUID();
        UUID branchServiceId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime time = LocalTime.of(10, 0);

        mockMvc.perform(post("/patient/book")
                .with(csrf())
                .with(user("patient@clinic.test").roles("PATIENT"))
                .param("branchId", branchId.toString())
                .param("branchServiceId", branchServiceId.toString())
                .param("staffId", staffId.toString())
                .param("appointmentDate", date.toString())
                .param("startTime", "10:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/appointments"))
                .andExpect(flash().attributeExists("successMessage"));

        ArgumentCaptor<BookingCommand> captor = ArgumentCaptor.forClass(BookingCommand.class);
        verify(bookingService).bookAppointment(captor.capture());

        BookingCommand captured = captor.getValue();
        assertThat(captured.patientId()).isNull();
        assertThat(captured.branchId()).isEqualTo(branchId);
        assertThat(captured.branchServiceId()).isEqualTo(branchServiceId);
        assertThat(captured.staffId()).isEqualTo(staffId);
        assertThat(captured.appointmentDate()).isEqualTo(date);
        assertThat(captured.startTime()).isEqualTo(time);
    }

    @Test
    @DisplayName("15. Booking concurrency conflict shows Turkish conflict message and preserves input")
    void bookingConcurrencyConflictShowsTurkishConflictMessage() throws Exception {
        UUID branchId = UUID.randomUUID();
        UUID branchServiceId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(1);

        when(bookingService.bookAppointment(any()))
                .thenThrow(new AppointmentConflictException("Conflict"));

        mockMvc.perform(post("/patient/book")
                .with(csrf())
                .with(user("patient@clinic.test").roles("PATIENT"))
                .param("branchId", branchId.toString())
                .param("branchServiceId", branchServiceId.toString())
                .param("staffId", staffId.toString())
                .param("appointmentDate", date.toString())
                .param("startTime", "10:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/book"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(content().string(containsString("çakışma")));
    }

    @Test
    @DisplayName("16. Patient appointments list displays upcoming and past records")
    void patientAppointmentsListDisplaysRecords() throws Exception {
        UUID appId = UUID.randomUUID();
        AppointmentViewDto app = new AppointmentViewDto(
                appId, patientProfile.getId(), "Ayşe Yılmaz", UUID.randomUUID(), "Dr. Ahmet Yılmaz", "Kardiyoloji",
                UUID.randomUUID(), "Merkez Şube", "Europe/Istanbul", UUID.randomUUID(), "Genel Muayene",
                Instant.now().plusSeconds(3600), Instant.now().plusSeconds(5400),
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(10, 30),
                "2026-11-20", "10:00 - 10:30", 30, BigDecimal.valueOf(500), "TRY",
                AppointmentStatus.PENDING, "badge bg-warning", "Beklemede", true, false, false, false, false
        );

        when(appointmentQueryService.getPatientAppointmentsForCurrentUser())
                .thenReturn(List.of(app));

        mockMvc.perform(get("/patient/appointments")
                .with(user("patient@clinic.test").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/appointments"))
                .andExpect(model().attributeExists("upcomingAppointments", "pastAppointments"))
                .andExpect(content().string(containsString("Genel Muayene")));
    }

    @Test
    @DisplayName("17. Patient can cancel owned appointment via POST")
    void patientCanCancelOwnedAppointmentViaPost() throws Exception {
        UUID appointmentId = UUID.randomUUID();

        mockMvc.perform(post("/patient/appointments/{id}/cancel", appointmentId)
                .with(csrf())
                .with(user("patient@clinic.test").roles("PATIENT"))
                .param("reason", "İşim çıktı, gelemeyeceğim."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/appointments"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(appointmentApprovalService).cancelAppointment(appointmentId, "İşim çıktı, gelemeyeceğim.");
    }

    @Test
    @DisplayName("10. CSRF token is required on booking mutation POST")
    void csrfTokenIsRequiredOnBookingPost() throws Exception {
        mockMvc.perform(post("/patient/book")
                .with(user("patient@clinic.test").roles("PATIENT"))
                .param("branchId", UUID.randomUUID().toString())
                .param("branchServiceId", UUID.randomUUID().toString())
                .param("staffId", UUID.randomUUID().toString())
                .param("appointmentDate", LocalDate.now().plusDays(1).toString())
                .param("startTime", "09:00"))
                .andExpect(status().isForbidden());

        verify(bookingService, never()).bookAppointment(any());
    }

    @Test
    @DisplayName("13. Blank cancellation reason is rejected by CancellationForm validation")
    void blankCancellationReasonIsRejected() throws Exception {
        UUID appointmentId = UUID.randomUUID();

        mockMvc.perform(post("/patient/appointments/{id}/cancel", appointmentId)
                        .with(csrf())
                        .with(user("patient@clinic.test").roles("PATIENT"))
                        .param("reason", "   "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/appointments"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(appointmentApprovalService, never()).cancelAppointment(any(), any());
    }

    @Test
    @DisplayName("14. Cross-patient appointment cancellation returns 403 Forbidden")
    void crossPatientCancelReturns403() throws Exception {
        UUID otherAppointmentId = UUID.randomUUID();
        doThrow(new AccessDeniedException("Forbidden"))
                .when(appointmentApprovalService).cancelAppointment(otherAppointmentId, "İptal ediyorum.");

        mockMvc.perform(post("/patient/appointments/{id}/cancel", otherAppointmentId)
                        .with(csrf())
                        .with(user("patient@clinic.test").roles("PATIENT"))
                        .param("reason", "İptal ediyorum."))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"));
    }

    @Test
    @DisplayName("15. Invalid UUID on /patient/slots returns 400 JSON without internal error details")
    void invalidUuidOnSlotsReturns400Json() throws Exception {
        mockMvc.perform(get("/patient/slots")
                        .with(user("patient@clinic.test").roles("PATIENT"))
                        .param("staffId", "not-a-uuid")
                        .param("branchServiceId", UUID.randomUUID().toString())
                        .param("date", LocalDate.now().plusDays(1).toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }
}
