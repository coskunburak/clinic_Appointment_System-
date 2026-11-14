package com.example.clinic.web.patient;

import com.example.clinic.booking.application.AppointmentApprovalService;
import com.example.clinic.booking.application.AppointmentConflictException;
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
import com.example.clinic.web.common.WebErrorMessageMapper;
import com.example.clinic.web.form.BookingForm;
import com.example.clinic.web.form.CancellationForm;
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
 * Spring MVC controller orchestrating patient appointment workflows (FR-027, TC-027).
 * Implements server-side booking wizard, slot availability endpoint, bounded appointment listing,
 * and reason-validated cancellation without swallowing security exceptions.
 */
@Controller
@RequestMapping("/patient")
public class PatientBookingController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final int DEFAULT_PAST_PAGE_SIZE = 15;

    private final BookingService bookingService;
    private final AppointmentApprovalService appointmentApprovalService;
    private final AppointmentQueryService appointmentQueryService;
    private final PatientBookingQueryService patientBookingQueryService;

    public PatientBookingController(BookingService bookingService,
                                    AppointmentApprovalService appointmentApprovalService,
                                    AppointmentQueryService appointmentQueryService,
                                    PatientBookingQueryService patientBookingQueryService) {
        this.bookingService = Objects.requireNonNull(bookingService);
        this.appointmentApprovalService = Objects.requireNonNull(appointmentApprovalService);
        this.appointmentQueryService = Objects.requireNonNull(appointmentQueryService);
        this.patientBookingQueryService = Objects.requireNonNull(patientBookingQueryService);
    }

    /**
     * Patient dashboard landing screen.
     */
    @GetMapping("/dashboard")
    public String patientDashboard(Model model) {
        List<AppointmentViewDto> appointments = appointmentQueryService.getPatientAppointmentsForCurrentUser();
        List<AppointmentViewDto> safeAppointments = appointments != null ? appointments : Collections.emptyList();

        List<AppointmentViewDto> upcoming = safeAppointments.stream()
                .filter(a -> !a.isPast() && (a.canCancel() || "CONFIRMED".equals(a.status().name())))
                .limit(3)
                .toList();

        model.addAttribute("upcomingAppointments", upcoming);
        model.addAttribute("totalCount", safeAppointments.size());
        model.addAttribute("pendingCount", safeAppointments.stream().filter(a -> "PENDING".equals(a.status().name())).count());
        return "patient/dashboard";
    }

    /**
     * Patient booking screen rendering selection filters and form elements (TC-027).
     */
    @GetMapping("/book")
    public String showBookingPage(@RequestParam(value = "branchId", required = false) UUID branchId,
                                  @RequestParam(value = "branchServiceId", required = false) UUID branchServiceId,
                                  @RequestParam(value = "staffId", required = false) UUID staffId,
                                  @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                  Model model) {

        LocalDate resolvedDate = (date != null) ? date : patientBookingQueryService.getDefaultBookingDate(branchId);
        populateBookingModel(model, branchId, branchServiceId, staffId, resolvedDate);

        if (!model.containsAttribute("bookingForm")) {
            BookingForm form = new BookingForm();
            form.setBranchId(branchId);
            form.setBranchServiceId(branchServiceId);
            form.setStaffId(staffId);
            form.setAppointmentDate(resolvedDate);
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
            LocalDate fallbackDate = form.getAppointmentDate() != null
                    ? form.getAppointmentDate()
                    : patientBookingQueryService.getDefaultBookingDate(form.getBranchId());
            populateBookingModel(model, form.getBranchId(), form.getBranchServiceId(), form.getStaffId(), fallbackDate);
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

        } catch (AppointmentConflictException | PatientOverlapException | BookingValidationException
                 | IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("errorMessage", WebErrorMessageMapper.mapBookingError(e));
            populateBookingModel(model, form.getBranchId(), form.getBranchServiceId(), form.getStaffId(), form.getAppointmentDate());
            return "patient/book";
        }
    }

    /**
     * Lists appointments owned by the authenticated patient with bounded past history pagination (FR-022, TC-022, TC-027).
     */
    @GetMapping("/appointments")
    public String listAppointments(@RequestParam(value = "page", defaultValue = "0") int page,
                                   @RequestParam(value = "size", defaultValue = "15") int size,
                                   Model model) {
        List<AppointmentViewDto> appointments = appointmentQueryService.getPatientAppointmentsForCurrentUser();
        List<AppointmentViewDto> safeAppointments = appointments != null ? appointments : Collections.emptyList();

        List<AppointmentViewDto> upcoming = safeAppointments.stream()
                .filter(a -> !a.isPast())
                .toList();

        List<AppointmentViewDto> allPast = safeAppointments.stream()
                .filter(AppointmentViewDto::isPast)
                .toList();

        int safeSize = Math.clamp(size, 1, 50);
        int totalPast = allPast.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalPast / safeSize));
        int safePage = Math.clamp(page, 0, totalPages - 1);
        int fromIdx = Math.min(safePage * safeSize, totalPast);
        int toIdx = Math.min(fromIdx + safeSize, totalPast);
        List<AppointmentViewDto> pastPage = allPast.subList(fromIdx, toIdx);

        model.addAttribute("upcomingAppointments", upcoming);
        model.addAttribute("pastAppointments", pastPage);
        model.addAttribute("totalPastCount", totalPast);
        model.addAttribute("pastPage", safePage);
        model.addAttribute("pastTotalPages", totalPages);
        model.addAttribute("pastPageSize", safeSize);
        model.addAttribute("allAppointments", safeAppointments);
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
                                    @Valid @ModelAttribute("cancellationForm") CancellationForm form,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String validationMsg = bindingResult.hasFieldErrors("reason") && bindingResult.getFieldError("reason") != null
                    ? bindingResult.getFieldError("reason").getDefaultMessage()
                    : "İptal gerekçesi boş bırakılamaz ve en fazla 500 karakter olabilir.";
            redirectAttributes.addFlashAttribute("errorMessage", validationMsg);
            return "redirect:/patient/appointments";
        }

        try {
            appointmentApprovalService.cancelAppointment(id, form.getReason().strip());
            redirectAttributes.addFlashAttribute("successMessage", "Randevunuz başarıyla iptal edildi.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    WebErrorMessageMapper.mapLifecycleError(e, "Randevu iptal edilemedi."));
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

        LocalDate effectiveDate = date != null ? date : patientBookingQueryService.getDefaultBookingDate(branchId);

        List<String> slotTimes = Collections.emptyList();
        if (staffId != null && branchServiceId != null && effectiveDate != null) {
            slotTimes = patientBookingQueryService.getAvailableSlotTimes(staffId, branchServiceId, effectiveDate)
                    .stream()
                    .map(t -> t.format(TIME_FORMATTER))
                    .toList();
        }
        model.addAttribute("availableSlots", slotTimes);

        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("selectedServiceId", branchServiceId);
        model.addAttribute("selectedStaffId", staffId);
        model.addAttribute("selectedDate", effectiveDate);

        BranchSummaryDto selectedBranch = branchId != null
                ? branches.stream().filter(b -> b.id().equals(branchId)).findFirst().orElse(null)
                : null;
        ServiceSummaryDto selectedService = branchServiceId != null
                ? services.stream().filter(s -> s.id().equals(branchServiceId)).findFirst().orElse(null)
                : null;
        StaffSummaryDto selectedStaff = staffId != null
                ? staffList.stream().filter(st -> st.id().equals(staffId)).findFirst().orElse(null)
                : null;

        model.addAttribute("selectedBranch", selectedBranch);
        model.addAttribute("selectedService", selectedService);
        model.addAttribute("selectedStaff", selectedStaff);
    }
}
