package com.example.clinic.web.staff;

import com.example.clinic.booking.application.AppointmentApprovalService;
import com.example.clinic.booking.application.AppointmentConflictException;
import com.example.clinic.booking.application.AppointmentQueryService;
import com.example.clinic.booking.application.dto.AppointmentViewDto;
import com.example.clinic.booking.application.dto.WeeklyDayScheduleDto;
import com.example.clinic.booking.domain.AppointmentDomainException;
import com.example.clinic.web.common.WebErrorMessageMapper;
import com.example.clinic.web.form.RejectionForm;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Spring MVC controller governing medical staff scheduling and appointment lifecycle actions (FR-027, TC-027).
 * Implements daily, weekly (7-day grouped), and pending queue agenda views along with approve,
 * reason-validated reject, and complete transitions without swallowing security exceptions.
 */
@Controller
@RequestMapping("/staff")
public class StaffScheduleController {

    private final AppointmentQueryService appointmentQueryService;
    private final AppointmentApprovalService appointmentApprovalService;

    public StaffScheduleController(AppointmentQueryService appointmentQueryService,
                                   AppointmentApprovalService appointmentApprovalService) {
        this.appointmentQueryService = Objects.requireNonNull(appointmentQueryService);
        this.appointmentApprovalService = Objects.requireNonNull(appointmentApprovalService);
    }

    @GetMapping("/dashboard")
    public String staffDashboard() {
        return "redirect:/staff/schedule";
    }

    /**
     * Renders clinician daily, weekly (7-day), or pending appointments queue (FR-023, TC-023, TC-027).
     */
    @GetMapping("/schedule")
    public String showSchedule(@RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               @RequestParam(value = "view", required = false, defaultValue = "daily") String view,
                               Model model) {

        LocalDate clinicToday = appointmentQueryService.getCurrentClinicDate();
        LocalDate targetDate = (date != null) ? date : (clinicToday != null ? clinicToday : LocalDate.now());
        String currentView = normalizeView(view);

        List<AppointmentViewDto> appointments = appointmentQueryService.getStaffAppointmentsForCurrentUser(targetDate, currentView);
        List<AppointmentViewDto> safeAppointments = appointments != null ? appointments : Collections.emptyList();

        LocalDate prevDate = "weekly".equals(currentView) ? targetDate.minusWeeks(1) : targetDate.minusDays(1);
        LocalDate nextDate = "weekly".equals(currentView) ? targetDate.plusWeeks(1) : targetDate.plusDays(1);

        LocalDate weekStart = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        long pendingCount = safeAppointments.stream().filter(a -> "PENDING".equals(a.status().name())).count();
        long confirmedCount = safeAppointments.stream().filter(a -> "CONFIRMED".equals(a.status().name())).count();
        long completedCount = safeAppointments.stream().filter(a -> "COMPLETED".equals(a.status().name())).count();

        List<WeeklyDayScheduleDto> weeklyDays = "weekly".equals(currentView)
                ? appointmentQueryService.groupAppointmentsByWeekDays(targetDate, safeAppointments)
                : Collections.emptyList();

        model.addAttribute("appointments", safeAppointments);
        model.addAttribute("weeklyDays", weeklyDays);
        model.addAttribute("selectedDate", targetDate);
        model.addAttribute("view", currentView);
        model.addAttribute("prevDate", prevDate);
        model.addAttribute("nextDate", nextDate);
        model.addAttribute("weekStartDate", weekStart);
        model.addAttribute("weekEndDate", weekEnd);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("confirmedCount", confirmedCount);
        model.addAttribute("completedCount", completedCount);

        if (!model.containsAttribute("rejectionForm")) {
            model.addAttribute("rejectionForm", new RejectionForm());
        }

        return "staff/schedule";
    }

    /**
     * Approves a PENDING appointment via POST (FR-018, TC-018, TC-027).
     */
    @PostMapping("/appointments/{id}/approve")
    public String approveAppointment(@PathVariable("id") UUID id,
                                     RedirectAttributes redirectAttributes) {
        try {
            appointmentApprovalService.approveAppointment(id);
            redirectAttributes.addFlashAttribute("successMessage", "Randevu başarıyla onaylandı (CONFIRMED).");
        } catch (AppointmentConflictException | AppointmentDomainException | IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    WebErrorMessageMapper.mapLifecycleError(e, "Randevu onaylanamadı."));
        }
        return "redirect:/staff/schedule";
    }

    /**
     * Rejects a PENDING appointment with server-validated mandatory reason via POST (FR-019, TC-019, TC-027).
     */
    @PostMapping("/appointments/{id}/reject")
    public String rejectAppointment(@PathVariable("id") UUID id,
                                    @Valid @ModelAttribute("rejectionForm") RejectionForm form,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String validationMsg = bindingResult.hasFieldErrors("reason") && bindingResult.getFieldError("reason") != null
                    ? bindingResult.getFieldError("reason").getDefaultMessage()
                    : "Randevu reddi için geçerli bir gerekçe belirtilmelidir (en fazla 500 karakter).";
            redirectAttributes.addFlashAttribute("errorMessage", validationMsg);
            return "redirect:/staff/schedule";
        }

        try {
            appointmentApprovalService.rejectAppointment(id, form.getReason().strip());
            redirectAttributes.addFlashAttribute("successMessage", "Randevu başarıyla reddedildi ve zaman aralığı serbest bırakıldı.");
        } catch (AppointmentDomainException | IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    WebErrorMessageMapper.mapLifecycleError(e, "Randevu reddedilemedi."));
        }
        return "redirect:/staff/schedule";
    }

    /**
     * Completes a past CONFIRMED appointment via POST (FR-021, TC-021).
     */
    @PostMapping("/appointments/{id}/complete")
    public String completeAppointment(@PathVariable("id") UUID id,
                                      RedirectAttributes redirectAttributes) {
        try {
            appointmentApprovalService.completeAppointment(id);
            redirectAttributes.addFlashAttribute("successMessage", "Randevu başarıyla tamamlandı olarak işaretlendi (COMPLETED).");
        } catch (AppointmentDomainException | IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    WebErrorMessageMapper.mapLifecycleError(e, "Randevu tamamlanamadı."));
        }
        return "redirect:/staff/schedule";
    }

    private String normalizeView(String view) {
        if (view == null) {
            return "daily";
        }
        String normalized = view.trim().toLowerCase(Locale.ROOT);
        if ("weekly".equals(normalized) || "pending".equals(normalized)) {
            return normalized;
        }
        return "daily";
    }
}
