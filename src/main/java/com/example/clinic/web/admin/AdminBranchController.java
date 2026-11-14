package com.example.clinic.web.admin;

import com.example.clinic.organization.application.OrganizationAdminService;
import com.example.clinic.organization.application.dto.BranchResponse;
import com.example.clinic.organization.application.dto.CreateBranchCommand;
import com.example.clinic.organization.application.dto.OpeningHoursDto;
import com.example.clinic.organization.application.dto.OrganizationResponse;
import com.example.clinic.web.common.WebErrorMessageMapper;
import com.example.clinic.web.form.BranchEditForm;
import com.example.clinic.web.form.BranchForm;
import com.example.clinic.web.form.BranchHoursForm;
import com.example.clinic.web.form.OrganizationUpdateForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Administrator controller for managing authorized organizations, branches,
 * branch profiles, and weekly opening hours (FR-028, TC-028).
 */
@Controller
@RequestMapping("/admin")
public class AdminBranchController {

    private final OrganizationAdminService organizationAdminService;

    public AdminBranchController(OrganizationAdminService organizationAdminService) {
        this.organizationAdminService = organizationAdminService;
    }

    @GetMapping("/branches")
    public String listBranches(Model model) {
        populateBranchesModel(model);
        if (!model.containsAttribute("branchForm")) {
            model.addAttribute("branchForm", new BranchForm());
        }
        return "admin/branches";
    }

    @PostMapping("/organizations/{id}/edit")
    public String updateOrganization(@PathVariable UUID id,
                                     @Valid @ModelAttribute("orgUpdateForm") OrganizationUpdateForm form,
                                     BindingResult bindingResult,
                                     RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("errorMessage", msg != null ? msg : "Kurum adı geçersiz.");
            return "redirect:/admin/branches";
        }
        try {
            organizationAdminService.updateOrganization(id, form.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Kurum bilgileri başarıyla güncellendi.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "Kurum bilgileri güncellenemedi."));
        }
        return "redirect:/admin/branches";
    }

    @PostMapping("/branches")
    public String createBranch(@Valid @ModelAttribute("branchForm") BranchForm form,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (form.getTimeZone() != null && !form.getTimeZone().isBlank()) {
            try {
                ZoneId.of(form.getTimeZone().strip());
            } catch (DateTimeException e) {
                bindingResult.rejectValue("timeZone", "timeZone.invalid",
                        "Geçerli bir IANA zaman dilimi giriniz (örnek: Europe/Istanbul).");
            }
        }

        if (bindingResult.hasErrors()) {
            populateBranchesModel(model);
            return "admin/branches";
        }

        try {
            organizationAdminService.createBranch(
                    form.getOrganizationId(),
                    new CreateBranchCommand(form.getName(), form.getTimeZone(), form.getAddress())
            );
            redirectAttributes.addFlashAttribute("successMessage", "Yeni şube başarıyla oluşturuldu.");
            return "redirect:/admin/branches";
        } catch (IllegalArgumentException | IllegalStateException e) {
            populateBranchesModel(model);
            model.addAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "Şube oluşturulamadı."));
            return "admin/branches";
        }
    }

    @PostMapping("/branches/{id}/edit")
    public String updateBranch(@PathVariable UUID id,
                               @Valid @ModelAttribute("branchEditForm") BranchEditForm form,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (form.getTimeZone() != null && !form.getTimeZone().isBlank()) {
            try {
                ZoneId.of(form.getTimeZone().strip());
            } catch (DateTimeException e) {
                bindingResult.rejectValue("timeZone", "timeZone.invalid",
                        "Geçerli bir IANA zaman dilimi giriniz (örnek: Europe/Istanbul).");
            }
        }
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("errorMessage", msg != null ? msg : "Şube bilgileri geçersiz.");
            return "redirect:/admin/branches";
        }
        try {
            organizationAdminService.updateBranch(id, form.getName(), form.getTimeZone(), form.getAddress());
            redirectAttributes.addFlashAttribute("successMessage", "Şube bilgileri başarıyla güncellendi.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "Şube bilgileri güncellenemedi."));
        }
        return "redirect:/admin/branches";
    }

    @PostMapping("/branches/{id}/activate")
    public String activateBranch(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            organizationAdminService.activateBranch(id);
            redirectAttributes.addFlashAttribute("successMessage", "Şube aktif duruma getirildi.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "Şube aktif edilemedi."));
        }
        return "redirect:/admin/branches";
    }

    @PostMapping("/branches/{id}/deactivate")
    public String deactivateBranch(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            organizationAdminService.deactivateBranch(id);
            redirectAttributes.addFlashAttribute("successMessage", "Şube pasif duruma alındı.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "Şube pasife alınamadı."));
        }
        return "redirect:/admin/branches";
    }

    @GetMapping("/hours")
    public String defaultHoursPage(Model model) {
        List<BranchResponse> branches = organizationAdminService.getAuthorizedBranches();
        if (branches.isEmpty()) {
            model.addAttribute("branch", null);
            model.addAttribute("branches", branches);
            model.addAttribute("openingHours", List.of());
            model.addAttribute("hoursForm", new BranchHoursForm());
            return "admin/branch-hours";
        }
        return "redirect:/admin/branches/" + branches.get(0).id() + "/hours";
    }

    @GetMapping("/branches/{id}/hours")
    public String showBranchHours(@PathVariable UUID id, Model model) {
        BranchResponse branch = organizationAdminService.getBranch(id);
        List<BranchResponse> branches = organizationAdminService.getAuthorizedBranches();
        List<OpeningHoursDto> hours = organizationAdminService.getOpeningHoursByBranch(id);

        model.addAttribute("branch", branch);
        model.addAttribute("branches", branches);
        model.addAttribute("openingHours", hours);

        if (!model.containsAttribute("hoursForm")) {
            BranchHoursForm form = new BranchHoursForm();
            form.setBranchId(id);
            model.addAttribute("hoursForm", form);
        }
        return "admin/branch-hours";
    }

    @PostMapping("/branches/{id}/hours")
    public String configureBranchHours(@PathVariable UUID id,
                                       @Valid @ModelAttribute("hoursForm") BranchHoursForm form,
                                       BindingResult bindingResult,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        if (form.getStartTime() != null && form.getEndTime() != null
                && !form.getStartTime().isBefore(form.getEndTime())) {
            bindingResult.rejectValue("endTime", "hours.order",
                    "Kapanış saati açılış saatinden sonra olmalıdır.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("branch", organizationAdminService.getBranch(id));
            model.addAttribute("branches", organizationAdminService.getAuthorizedBranches());
            model.addAttribute("openingHours", organizationAdminService.getOpeningHoursByBranch(id));
            return "admin/branch-hours";
        }

        try {
            List<OpeningHoursDto> existingHours = new ArrayList<>(organizationAdminService.getOpeningHoursByBranch(id));
            existingHours.removeIf(h -> h.weekday() == form.getWeekday());
            existingHours.add(new OpeningHoursDto(form.getWeekday(), form.getStartTime(), form.getEndTime()));
            organizationAdminService.configureOpeningHours(id, existingHours);

            redirectAttributes.addFlashAttribute("successMessage", "Şube çalışma saatleri kaydedildi.");
            return "redirect:/admin/branches/" + id + "/hours";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("branch", organizationAdminService.getBranch(id));
            model.addAttribute("branches", organizationAdminService.getAuthorizedBranches());
            model.addAttribute("openingHours", organizationAdminService.getOpeningHoursByBranch(id));
            model.addAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "Çalışma saatleri güncellenemedi."));
            return "admin/branch-hours";
        }
    }

    @PostMapping("/branches/{id}/hours/{weekday}/delete")
    public String deleteBranchHour(@PathVariable UUID id,
                                   @PathVariable short weekday,
                                   RedirectAttributes redirectAttributes) {
        if (weekday < 1 || weekday > 7) {
            redirectAttributes.addFlashAttribute("errorMessage", "Geçersiz gün değeri.");
            return "redirect:/admin/branches/" + id + "/hours";
        }
        try {
            organizationAdminService.getBranch(id);
            List<OpeningHoursDto> remaining = organizationAdminService.getOpeningHoursByBranch(id).stream()
                    .filter(h -> h.weekday() != weekday)
                    .toList();
            organizationAdminService.configureOpeningHours(id, remaining);
            redirectAttributes.addFlashAttribute("successMessage", "Seçili güne ait çalışma saati kaldırıldı.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "Çalışma saati silinemedi."));
        }
        return "redirect:/admin/branches/" + id + "/hours";
    }

    private void populateBranchesModel(Model model) {
        List<OrganizationResponse> organizations = organizationAdminService.getAuthorizedOrganizations();
        List<OrganizationResponse> orgWideOrgs = organizationAdminService.getOrgWideAuthorizedOrganizations();
        Set<UUID> orgWideOrgIds = orgWideOrgs.stream()
                .map(OrganizationResponse::id)
                .collect(Collectors.toSet());
        List<BranchResponse> branches = organizationAdminService.getAuthorizedBranches();

        model.addAttribute("organizations", organizations);
        model.addAttribute("orgWideOrganizations", orgWideOrgs);
        model.addAttribute("orgWideOrgIds", orgWideOrgIds);
        model.addAttribute("branches", branches);
    }
}
