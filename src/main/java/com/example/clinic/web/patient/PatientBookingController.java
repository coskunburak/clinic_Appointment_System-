package com.example.clinic.web.patient;

import com.example.clinic.booking.application.AppointmentConflictException;
import com.example.clinic.booking.application.AppointmentApprovalService;
import com.example.clinic.booking.application.AppointmentQueryService;
import com.example.clinic.booking.application.BookingCommand;
import com.example.clinic.booking.application.BookingService;
import com.example.clinic.booking.application.BookingValidationException;
import com.example.clinic.booking.application.PatientBookingQueryService;
import com.example.clinic.booking.application.PatientOverlapException;
import com.example.clinic.booking.application.dto.AppointmentViewDto;
import com.example.clinic.booking.application.dto.BranchSummaryDto;
import com.example.clinic.booking.application.dto.ServiceSummaryDto;
import com.example.clinic.booking.application.dto.StaffSummaryDto;
import com.example.clinic.common.security.SecurityContextFacade;
import com.example.clinic.web.form.BookingForm;
import com.example.clinic.web.form.CancellationForm;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Spring MVC controller orchestrating patient appointment workflows (WP-S08-02, FR-027, TC-027).
 * Implements server-side booking wizard, slot availability endpoint, appointment listing, and cancellation.
 */
@Controller
@RequestMapping("/patient")
public class PatientBookingController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final BookingService bookingService;
    private final AppointmentApprovalService appointmentApprovalService;
    private final AppointmentQueryService appointmentQueryService;
    private final PatientBookingQueryService patientBookingQueryService;
    private final SecurityContextFacade securityContextFacade;

    public PatientBookingController(BookingService bookingService,
                                    AppointmentApprovalService appointmentApprovalService,
                                    AppointmentQueryService appointmentQueryService,
                                    PatientBookingQueryService patientBookingQueryService,
                                    SecurityContextFacade securityContextFacade) {
        this.bookingService = Objects.requireNonNull(bookingService);
        this.appointmentApprovalService = Objects.requireNonNull(appointmentApprovalService);
        this.appointmentQueryService = Objects.requireNonNull(appointmentQueryService);
        this.patientBookingQueryService = Objects.requireNonNull(patientBookingQueryService);
        this.securityContextFacade = Objects.requireNonNull(securityContextFacade);
    }

    /**
     * Patient dashboard landing screen.
     */
    @GetMapping("/dashboard")
    public String patientDashboard(Model model) {
        try {
            List<AppointmentViewDto> appointments = appointmentQueryService.getPatientAppointmentsForCurrentUser();
            List<AppointmentViewDto> upcoming = appointments.stream()
                    .filter(a -> !a.isPast() && (a.canCancel() || a.status().name().equals("CONFIRMED")))
                    .limit(3)
                    .toList();

            model.addAttribute("upcomingAppointments", upcoming);
            model.addAttribute("totalCount", appointments.size());
            model.addAttribute("pendingCount", appointments.stream().filter(a -> "PENDING".equals(a.status().name())).count());
        } catch (Exception e) {
            model.addAttribute("upcomingAppointments", Collections.emptyList());
            model.addAttribute("totalCount", 0);
            model.addAttribute("pendingCount", 0);
        }
        return "patient/dashboard";
    }

    /**
     * Patient booking screen rendering selection filters and form elements (WP-S08-02, TC-027).
     */
    @GetMapping("/book")
    public String showBookingPage(@RequestParam(value = "branchId", required = false) UUID branchId,
                                  @RequestParam(value = "branchServiceId", required = false) UUID branchServiceId,
                                  @RequestParam(value = "staffId", required = false) UUID staffId,
                                  @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                  Model model) {

        populateBookingModel(model, branchId, branchServiceId, staffId, date);

        if (!model.containsAttribute("bookingForm")) {
            BookingForm form = new BookingForm();
            form.setBranchId(branchId);
            form.setBranchServiceId(branchServiceId);
            form.setStaffId(staffId);
            form.setAppointmentDate(date != null ? date : LocalDate.now());
            model.addAttribute("bookingForm", form);
        }

        return "patient/book";
    }

    /**
     * AJAX slot calculation endpoint returning available discrete start times for selected clinician and service.
     */
    @GetMapping("/slots")
    @ResponseBody
    public List<String> getAvailableSlots(@RequestParam("staffId") UUID staffId,
                                         @RequestParam("branchServiceId") UUID branchServiceId,
                                         @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<LocalTime> times = patientBookingQueryService.getAvailableSlotTimes(staffId, branchServiceId, date);
        return times.stream()
                .map(t -> t.format(TIME_FORMATTER))
                .toList();
    }

    /**
     * Submits a new appointment booking with authoritative server-side validation and locking (FR-012..FR-017).
     */
    @PostMapping("/book")
    public String submitBooking(@Valid @ModelAttribute("bookingForm") BookingForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            populateBookingModel(model, form.getBranchId(), form.getBranchServiceId(), form.getStaffId(), form.getAppointmentDate());
            return "patient/book";
        }

        BookingCommand command = new BookingCommand(
                form.getStaffId(),
                form.getBranchId(),
                form.getBranchServiceId(),
                form.getAppointmentDate(),
                form.getStartTime()
        );

        try {
            bookingService.bookAppointment(command);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Randevunuz başarıyla oluşturuldu. Başlangıç durumu: Beklemede (PENDING). Hekim onayından sonra kesinleşecektir.");
            return "redirect:/patient/appointments";

        } catch (AppointmentConflictException | PatientOverlapException e) {
            model.addAttribute("errorMessage",
                    "Seçilen zaman diliminde randevu çakışması tespit edildi. Lütfen listeden başka bir randevu saati seçiniz.");
            populateBookingModel(model, form.getBranchId(), form.getBranchServiceId(), form.getStaffId(), form.getAppointmentDate());
            return "patient/book";

        } catch (BookingValidationException e) {
            model.addAttribute("errorMessage", "Rezervasyon kuralı ihlali: " + e.getMessage());
            populateBookingModel(model, form.getBranchId(), form.getBranchServiceId(), form.getStaffId(), form.getAppointmentDate());
            return "patient/book";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Randevu oluşturulamadı: " + e.getMessage());
            populateBookingModel(model, form.getBranchId(), form.getBranchServiceId(), form.getStaffId(), form.getAppointmentDate());
            return "patient/book";
        }
    }

    /**
     * Lists appointments owned by the authenticated patient (FR-022, TC-022, TC-027).
     */
    @GetMapping("/appointments")
    public String listAppointments(Model model) {
        List<AppointmentViewDto> appointments = appointmentQueryService.getPatientAppointmentsForCurrentUser();

        List<AppointmentViewDto> upcoming = appointments.stream()
                .filter(a -> !a.isPast())
                .toList();

        List<AppointmentViewDto> past = appointments.stream()
                .filter(AppointmentViewDto::isPast)
                .toList();

        model.addAttribute("upcomingAppointments", upcoming);
        model.addAttribute("pastAppointments", past);
        model.addAttribute("allAppointments", appointments);
        if (!model.containsAttribute("cancellationForm")) {
            model.addAttribute("cancellationForm", new CancellationForm());
        }

        return "patient/appointments";
    }

    /**
     * Cancels an appointment owned by the authenticated patient before its start time (FR-020, TC-020).
     */
    @PostMapping("/appointments/{id}/cancel")
    public String cancelAppointment(@PathVariable("id") UUID id,
                                    @RequestParam(value = "reason", required = false, defaultValue = "Hasta tarafından randevu iptal edildi.") String reason,
                                    RedirectAttributes redirectAttributes) {
        try {
            String trimmedReason = (reason != null && !reason.isBlank()) ? reason.strip() : "Hasta tarafından randevu iptal edildi.";
            appointmentApprovalService.cancelAppointment(id, trimmedReason);
            redirectAttributes.addFlashAttribute("successMessage", "Randevunuz başarıyla iptal edildi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Randevu iptal edilemedi: " + e.getMessage());
        }
        return "redirect:/patient/appointments";
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void populateBookingModel(Model model, UUID branchId, UUID branchServiceId, UUID staffId, LocalDate date) {
        List<BranchSummaryDto> branches = patientBookingQueryService.getActiveBranches();
        model.addAttribute("branches", branches);

        List<ServiceSummaryDto> services = Collections.emptyList();
        if (branchId != null) {
            services = patientBookingQueryService.getActiveServicesForBranch(branchId);
        }
        model.addAttribute("services", services);

        List<StaffSummaryDto> staffList = Collections.emptyList();
        if (branchServiceId != null) {
            staffList = patientBookingQueryService.getEligibleStaffForService(branchServiceId);
        }
        model.addAttribute("staffList", staffList);

        List<String> slotTimes = Collections.emptyList();
        if (staffId != null && branchServiceId != null && date != null) {
            slotTimes = patientBookingQueryService.getAvailableSlotTimes(staffId, branchServiceId, date)
                    .stream()
                    .map(t -> t.format(TIME_FORMATTER))
                    .toList();
        }
        model.addAttribute("availableSlots", slotTimes);

        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("selectedServiceId", branchServiceId);
        model.addAttribute("selectedStaffId", staffId);
        model.addAttribute("selectedDate", date != null ? date : LocalDate.now());
    }
}
