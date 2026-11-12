package com.example.clinic.web.staff;

import com.example.clinic.booking.application.AppointmentApprovalService;
import com.example.clinic.booking.application.AppointmentQueryService;
import com.example.clinic.booking.application.dto.AppointmentViewDto;
import com.example.clinic.web.form.RejectionForm;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Spring MVC controller governing medical staff scheduling and appointment lifecycle actions (WP-S08-03, FR-027, TC-027).
 * Implements daily/weekly agenda views, approval, reason-validated rejection, and completion actions.
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
     * Renders clinician daily or weekly schedule (FR-023, TC-023, TC-027).
     */
    @GetMapping("/schedule")
    public String showSchedule(@RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               @RequestParam(value = "view", required = false, defaultValue = "daily") String view,
                               Model model) {

        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        String currentView = ("weekly".equalsIgnoreCase(view)) ? "weekly" : "daily";

        List<AppointmentViewDto> appointments = appointmentQueryService.getStaffAppointmentsForCurrentUser(targetDate, currentView);

        LocalDate prevDate = "weekly".equalsIgnoreCase(currentView) ? targetDate.minusWeeks(1) : targetDate.minusDays(1);
        LocalDate nextDate = "weekly".equalsIgnoreCase(currentView) ? targetDate.plusWeeks(1) : targetDate.plusDays(1);

        long pendingCount = appointments.stream().filter(a -> "PENDING".equals(a.status().name())).count();
        long confirmedCount = appointments.stream().filter(a -> "CONFIRMED".equals(a.status().name())).count();
        long completedCount = appointments.stream().filter(a -> "COMPLETED".equals(a.status().name())).count();

        model.addAttribute("appointments", appointments);
        model.addAttribute("selectedDate", targetDate);
        model.addAttribute("view", currentView);
        model.addAttribute("prevDate", prevDate);
        model.addAttribute("nextDate", nextDate);
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
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Randevu onaylanamadı: " + e.getMessage());
        }
        return "redirect:/staff/schedule";
    }

    /**
     * Rejects a PENDING appointment with mandatory reason via POST (FR-019, TC-019, TC-027).
     */
    @PostMapping("/appointments/{id}/reject")
    public String rejectAppointment(@PathVariable("id") UUID id,
                                    @RequestParam("reason") String reason,
                                    RedirectAttributes redirectAttributes) {
        if (reason == null || reason.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Randevu reddi için geçerli bir gerekçe belirtilmelidir.");
            return "redirect:/staff/schedule";
        }

        String trimmedReason = reason.strip();
        if (trimmedReason.length() > 500) {
            redirectAttributes.addFlashAttribute("errorMessage", "Red gerekçesi 500 karakterden uzun olamaz.");
            return "redirect:/staff/schedule";
        }

        try {
            appointmentApprovalService.rejectAppointment(id, trimmedReason);
            redirectAttributes.addFlashAttribute("successMessage", "Randevu başarıyla reddedildi ve zaman aralığı serbest bırakıldı.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Randevu reddedilemedi: " + e.getMessage());
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
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Randevu tamamlanamadı: " + e.getMessage());
        }
        return "redirect:/staff/schedule";
    }
}
