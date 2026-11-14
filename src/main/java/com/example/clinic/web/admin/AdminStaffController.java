package com.example.clinic.web.admin;

import com.example.clinic.catalog.application.CatalogManagementService;
import com.example.clinic.catalog.application.dto.BranchServiceResponse;
import com.example.clinic.identity.domain.StaffType;
import com.example.clinic.organization.application.OrganizationAdminService;
import com.example.clinic.organization.application.dto.BranchResponse;
import com.example.clinic.organization.application.dto.DepartmentResponse;
import com.example.clinic.staff.application.StaffManagementService;
import com.example.clinic.staff.application.dto.AssignStaffCommand;
import com.example.clinic.staff.application.dto.ConfigureEligibilityCommand;
import com.example.clinic.staff.application.dto.ConfigureWeeklyAvailabilityCommand;
import com.example.clinic.staff.application.dto.CreateAvailabilityExceptionCommand;
import com.example.clinic.staff.application.dto.CreateStaffCommand;
import com.example.clinic.staff.application.dto.StaffAssignmentResponse;
import com.example.clinic.staff.application.dto.StaffProfileResponse;
import com.example.clinic.staff.application.dto.WeeklyAvailabilityItem;
import com.example.clinic.staff.application.dto.WeeklyAvailabilityResponse;
import com.example.clinic.staff.domain.AvailabilityExceptionKind;
import com.example.clinic.web.common.WebErrorMessageMapper;
import com.example.clinic.web.form.StaffAssignmentForm;
import com.example.clinic.web.form.StaffEligibilityForm;
import com.example.clinic.web.form.StaffExceptionForm;
import com.example.clinic.web.form.StaffRegistrationForm;
import com.example.clinic.web.form.WeeklyAvailabilityForm;
import jakarta.validation.Valid;
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

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Administrator controller for tenant-scoped staff registration, branch/department assignments,
 * service eligibilities, weekly schedules, and leave/overtime exceptions (FR-028, TC-028).
 */
@Controller
@RequestMapping("/admin")
public class AdminStaffController {

    private final StaffManagementService staffManagementService;
    private final OrganizationAdminService organizationAdminService;
    private final CatalogManagementService catalogManagementService;

    public AdminStaffController(StaffManagementService staffManagementService,
                                OrganizationAdminService organizationAdminService,
                                CatalogManagementService catalogManagementService) {
        this.staffManagementService = staffManagementService;
        this.organizationAdminService = organizationAdminService;
        this.catalogManagementService = catalogManagementService;
    }

    @GetMapping("/staff")
    public String listStaff(Model model) {
        populateStaffOverviewModel(model);
        ensureStaffOverviewForms(model);
        return "admin/staff";
    }

    @PostMapping("/staff")
    public String registerStaff(@Valid @ModelAttribute("staffForm") StaffRegistrationForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (form.getStaffType() == StaffType.DOCTOR
                && (form.getSpecialty() == null || form.getSpecialty().isBlank())) {
            bindingResult.rejectValue("specialty", "specialty.required",
                    "Hekim (DOCTOR) kaydı için uzmanlık alanı zorunludur.");
        }

        if (bindingResult.hasErrors()) {
            populateStaffOverviewModel(model);
            ensureStaffOverviewForms(model);
            return "admin/staff";
        }

        try {
            BranchResponse branch = organizationAdminService.getBranch(form.getBranchId());
            staffManagementService.registerStaff(new CreateStaffCommand(
                    branch.organizationId(),
                    branch.id(),
                    form.getEmail(),
                    form.getPassword(),
                    form.getFullName(),
                    form.getPhone(),
                    form.getStaffType().name(),
                    form.getSpecialty()
            ));
            redirectAttributes.addFlashAttribute("successMessage", "Personel kaydı oluşturuldu.");
            return "redirect:/admin/staff";
        } catch (IllegalArgumentException | IllegalStateException e) {
            populateStaffOverviewModel(model);
            ensureStaffOverviewForms(model);
            model.addAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "Personel kaydı oluşturulamadı."));
            return "admin/staff";
        }
    }

    @PostMapping("/staff/assign")
    public String assignStaffToBranch(@Valid @ModelAttribute("assignmentForm") StaffAssignmentForm form,
                                      BindingResult bindingResult,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateStaffOverviewModel(model);
            ensureStaffOverviewForms(model);
            return "admin/staff";
        }

        try {
            staffManagementService.assignStaffToBranch(new AssignStaffCommand(
                    form.getStaffId(),
                    form.getBranchId(),
                    form.getDepartmentId()
            ));
            redirectAttributes.addFlashAttribute("successMessage", "Personel şube ve bölüme atandı.");
            return "redirect:/admin/staff";
        } catch (IllegalArgumentException | IllegalStateException e) {
            populateStaffOverviewModel(model);
            ensureStaffOverviewForms(model);
            model.addAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "Personel ataması başarısız."));
            return "admin/staff";
        }
    }

    @PostMapping("/staff/eligibility")
    public String configureEligibility(@Valid @ModelAttribute("eligibilityForm") StaffEligibilityForm form,
                                       BindingResult bindingResult,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateStaffOverviewModel(model);
            ensureStaffOverviewForms(model);
            return "admin/staff";
        }

        try {
            List<UUID> serviceIds = form.getBranchServiceIds() != null
                    ? form.getBranchServiceIds()
                    : Collections.emptyList();
            staffManagementService.configureEligibility(
                    new ConfigureEligibilityCommand(form.getAssignmentId(), serviceIds)
            );
            redirectAttributes.addFlashAttribute("successMessage", "Personel hizmet yetkinlikleri güncellendi.");
            return "redirect:/admin/staff";
        } catch (IllegalArgumentException | IllegalStateException e) {
            populateStaffOverviewModel(model);
            ensureStaffOverviewForms(model);
            model.addAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "Yetkinlik güncellemesi başarısız."));
            return "admin/staff";
        }
    }

    @GetMapping("/staff/{id}/availability")
    public String showStaffAvailability(@PathVariable UUID id,
                                        @RequestParam(required = false) UUID assignmentId,
                                        Model model) {
        StaffProfileResponse staff = staffManagementService.getAuthorizedStaffProfile(id);
        List<StaffAssignmentResponse> assignments = staffManagementService.getAuthorizedStaffAssignmentsByStaff(id);

        UUID selectedAssignmentId = resolveSelectedAssignmentId(assignments, assignmentId);
        populateAvailabilityModel(model, staff, assignments, selectedAssignmentId);
        ensureAvailabilityForms(model, selectedAssignmentId);

        return "admin/staff-availability";
    }

    @PostMapping("/staff/assignments/{assignmentId}/weekly-availability")
    public String addWeeklyAvailability(@PathVariable UUID assignmentId,
                                        @RequestParam(required = false) UUID staffId,
                                        @Valid @ModelAttribute("weeklyForm") WeeklyAvailabilityForm form,
                                        BindingResult bindingResult,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        if (form.getStartTime() != null && form.getEndTime() != null
                && !form.getStartTime().isBefore(form.getEndTime())) {
            bindingResult.rejectValue("endTime", "weekly.order",
                    "Bitiş saati başlangıç saatinden sonra olmalıdır.");
        }

        UUID resolvedStaffId = resolveStaffIdForAssignment(assignmentId, staffId);

        if (bindingResult.hasErrors()) {
            if (resolvedStaffId != null) {
                StaffProfileResponse staff = staffManagementService.getAuthorizedStaffProfile(resolvedStaffId);
                List<StaffAssignmentResponse> assignments = staffManagementService.getAuthorizedStaffAssignmentsByStaff(resolvedStaffId);
                populateAvailabilityModel(model, staff, assignments, assignmentId);
                ensureAvailabilityForms(model, assignmentId);
                return "admin/staff-availability";
            }
            redirectAttributes.addFlashAttribute("errorMessage", "Haftalık çalışma saati formunda geçersiz alanlar var.");
            return "redirect:/admin/staff";
        }

        try {
            List<WeeklyAvailabilityResponse> existing = staffManagementService
                    .getAuthorizedWeeklyAvailabilitiesByAssignment(assignmentId);
            List<WeeklyAvailabilityItem> items = new ArrayList<>();
            for (WeeklyAvailabilityResponse w : existing) {
                items.add(new WeeklyAvailabilityItem(w.weekday(), w.startTime(), w.endTime()));
            }
            items.add(new WeeklyAvailabilityItem(form.getWeekday(), form.getStartTime(), form.getEndTime()));

            staffManagementService.configureWeeklyAvailability(
                    new ConfigureWeeklyAvailabilityCommand(assignmentId, items)
            );
            redirectAttributes.addFlashAttribute("successMessage", "Haftalık çalışma saati kaydedildi.");
            return redirectToStaffAvailability(resolvedStaffId, assignmentId);
        } catch (IllegalArgumentException | IllegalStateException e) {
            if (resolvedStaffId != null) {
                StaffProfileResponse staff = staffManagementService.getAuthorizedStaffProfile(resolvedStaffId);
                List<StaffAssignmentResponse> assignments = staffManagementService.getAuthorizedStaffAssignmentsByStaff(resolvedStaffId);
                populateAvailabilityModel(model, staff, assignments, assignmentId);
                ensureAvailabilityForms(model, assignmentId);
                model.addAttribute("errorMessage",
                        WebErrorMessageMapper.mapAdminError(e, "Haftalık çalışma saati eklenemedi."));
                return "admin/staff-availability";
            }
            redirectAttributes.addFlashAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "Haftalık çalışma saati eklenemedi."));
            return "redirect:/admin/staff";
        }
    }

    @PostMapping("/staff/assignments/{assignmentId}/weekly-availability/{availabilityId}/delete")
    public String deleteWeeklyAvailability(@PathVariable UUID assignmentId,
                                           @PathVariable UUID availabilityId,
                                           @RequestParam(required = false) UUID staffId,
                                           RedirectAttributes redirectAttributes) {
        UUID resolvedStaffId = resolveStaffIdForAssignment(assignmentId, staffId);
        try {
            staffManagementService.deleteWeeklyAvailability(assignmentId, availabilityId);
            redirectAttributes.addFlashAttribute("successMessage", "Haftalık çalışma dilimi silindi.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "Haftalık çalışma dilimi silinemedi."));
        }
        return redirectToStaffAvailability(resolvedStaffId, assignmentId);
    }

    @PostMapping("/staff/assignments/{assignmentId}/exceptions")
    public String addException(@PathVariable UUID assignmentId,
                               @RequestParam(required = false) UUID staffId,
                               @Valid @ModelAttribute("exceptionForm") StaffExceptionForm form,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        LocalTime startTime = form.getStartTime();
        LocalTime endTime = form.getEndTime();
        if (form.getKind() == AvailabilityExceptionKind.FULL_DAY_LEAVE) {
            startTime = null;
            endTime = null;
        } else if (form.getKind() != null) {
            if (startTime == null) {
                bindingResult.rejectValue("startTime", "exception.startTime.required",
                        "Bu istisna türü için başlangıç saati zorunludur.");
            }
            if (endTime == null) {
                bindingResult.rejectValue("endTime", "exception.endTime.required",
                        "Bu istisna türü için bitiş saati zorunludur.");
            }
            if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
                bindingResult.rejectValue("endTime", "exception.order",
                        "Bitiş saati başlangıç saatinden sonra olmalıdır.");
            }
        }

        UUID resolvedStaffId = resolveStaffIdForAssignment(assignmentId, staffId);

        if (bindingResult.hasErrors()) {
            if (resolvedStaffId != null) {
                StaffProfileResponse staff = staffManagementService.getAuthorizedStaffProfile(resolvedStaffId);
                List<StaffAssignmentResponse> assignments = staffManagementService.getAuthorizedStaffAssignmentsByStaff(resolvedStaffId);
                populateAvailabilityModel(model, staff, assignments, assignmentId);
                ensureAvailabilityForms(model, assignmentId);
                return "admin/staff-availability";
            }
            redirectAttributes.addFlashAttribute("errorMessage", "İstisna formunda geçersiz alanlar var.");
            return "redirect:/admin/staff";
        }

        try {
            staffManagementService.addAvailabilityException(new CreateAvailabilityExceptionCommand(
                    assignmentId,
                    form.getLocalDate(),
                    form.getKind(),
                    startTime,
                    endTime,
                    form.getReason()
            ));
            redirectAttributes.addFlashAttribute("successMessage", "İzin veya mesai istisnası kaydedildi.");
            return redirectToStaffAvailability(resolvedStaffId, assignmentId);
        } catch (IllegalArgumentException | IllegalStateException e) {
            if (resolvedStaffId != null) {
                StaffProfileResponse staff = staffManagementService.getAuthorizedStaffProfile(resolvedStaffId);
                List<StaffAssignmentResponse> assignments = staffManagementService.getAuthorizedStaffAssignmentsByStaff(resolvedStaffId);
                populateAvailabilityModel(model, staff, assignments, assignmentId);
                ensureAvailabilityForms(model, assignmentId);
                model.addAttribute("errorMessage",
                        WebErrorMessageMapper.mapAdminError(e, "İzin veya mesai istisnası eklenemedi."));
                return "admin/staff-availability";
            }
            redirectAttributes.addFlashAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "İzin veya mesai istisnası eklenemedi."));
            return "redirect:/admin/staff";
        }
    }

    @PostMapping("/staff/assignments/{assignmentId}/exceptions/{exceptionId}/delete")
    public String deleteException(@PathVariable UUID assignmentId,
                                  @PathVariable UUID exceptionId,
                                  @RequestParam(required = false) UUID staffId,
                                  RedirectAttributes redirectAttributes) {
        UUID resolvedStaffId = resolveStaffIdForAssignment(assignmentId, staffId);
        try {
            staffManagementService.deleteAvailabilityException(assignmentId, exceptionId);
            redirectAttributes.addFlashAttribute("successMessage", "İzin veya mesai istisnası silindi.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "İstisna kaydı silinemedi."));
        }
        return redirectToStaffAvailability(resolvedStaffId, assignmentId);
    }

    private void populateStaffOverviewModel(Model model) {
        List<BranchResponse> branches = organizationAdminService.getAuthorizedBranches();
        List<DepartmentResponse> departments = new ArrayList<>();
        List<BranchServiceResponse> branchServices = new ArrayList<>();
        for (BranchResponse branch : branches) {
            departments.addAll(organizationAdminService.getDepartmentsByBranch(branch.id()));
            branchServices.addAll(catalogManagementService.getBranchServicesByBranch(branch.id()));
        }

        List<StaffProfileResponse> staffProfiles = staffManagementService.getAuthorizedStaffProfiles();
        List<StaffAssignmentResponse> assignments = staffManagementService.getAuthorizedStaffAssignments();

        model.addAttribute("branches", branches);
        model.addAttribute("departments", departments);
        model.addAttribute("staffProfiles", staffProfiles);
        model.addAttribute("assignments", assignments);
        model.addAttribute("branchServices", branchServices);
    }

    private void ensureStaffOverviewForms(Model model) {
        if (!model.containsAttribute("staffForm")) {
            model.addAttribute("staffForm", new StaffRegistrationForm());
        }
        if (!model.containsAttribute("assignmentForm")) {
            model.addAttribute("assignmentForm", new StaffAssignmentForm());
        }
        if (!model.containsAttribute("eligibilityForm")) {
            model.addAttribute("eligibilityForm", new StaffEligibilityForm());
        }
    }

    private UUID resolveSelectedAssignmentId(List<StaffAssignmentResponse> assignments, UUID requestedAssignmentId) {
        if (requestedAssignmentId != null) {
            for (StaffAssignmentResponse a : assignments) {
                if (Objects.equals(a.id(), requestedAssignmentId)) {
                    return requestedAssignmentId;
                }
            }
            // Trigger authoritative AccessDeniedException if assignment belongs to another tenant
            staffManagementService.getAuthorizedWeeklyAvailabilitiesByAssignment(requestedAssignmentId);
            throw new IllegalArgumentException("Assignment does not belong to requested staff member.");
        }
        return assignments.isEmpty() ? null : assignments.get(0).id();
    }

    private void populateAvailabilityModel(Model model,
                                           StaffProfileResponse staff,
                                           List<StaffAssignmentResponse> assignments,
                                           UUID selectedAssignmentId) {
        model.addAttribute("staff", staff);
        model.addAttribute("assignments", assignments);
        model.addAttribute("selectedAssignmentId", selectedAssignmentId);

        if (selectedAssignmentId != null) {
            model.addAttribute("weeklyAvailabilities",
                    staffManagementService.getAuthorizedWeeklyAvailabilitiesByAssignment(selectedAssignmentId));
            model.addAttribute("exceptions",
                    staffManagementService.getAuthorizedAvailabilityExceptionsByAssignment(selectedAssignmentId));
        } else {
            model.addAttribute("weeklyAvailabilities", Collections.emptyList());
            model.addAttribute("exceptions", Collections.emptyList());
        }
    }

    private void ensureAvailabilityForms(Model model, UUID selectedAssignmentId) {
        if (!model.containsAttribute("weeklyForm")) {
            WeeklyAvailabilityForm wForm = new WeeklyAvailabilityForm();
            wForm.setAssignmentId(selectedAssignmentId);
            model.addAttribute("weeklyForm", wForm);
        }
        if (!model.containsAttribute("exceptionForm")) {
            StaffExceptionForm eForm = new StaffExceptionForm();
            eForm.setAssignmentId(selectedAssignmentId);
            model.addAttribute("exceptionForm", eForm);
        }
    }

    private UUID resolveStaffIdForAssignment(UUID assignmentId, UUID explicitStaffId) {
        if (explicitStaffId != null) {
            return explicitStaffId;
        }
        return staffManagementService.getAuthorizedStaffAssignments().stream()
                .filter(a -> Objects.equals(a.id(), assignmentId))
                .map(StaffAssignmentResponse::staffId)
                .findFirst()
                .orElse(null);
    }

    private String redirectToStaffAvailability(UUID staffId, UUID assignmentId) {
        if (staffId == null) {
            return "redirect:/admin/staff";
        }
        return assignmentId != null
                ? "redirect:/admin/staff/" + staffId + "/availability?assignmentId=" + assignmentId
                : "redirect:/admin/staff/" + staffId + "/availability";
    }
}
