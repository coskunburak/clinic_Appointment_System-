package com.example.clinic.web.admin;

import com.example.clinic.organization.application.OrganizationAdminService;
import com.example.clinic.organization.application.dto.BranchResponse;
import com.example.clinic.organization.application.dto.DepartmentResponse;
import com.example.clinic.web.common.WebErrorMessageMapper;
import com.example.clinic.web.form.DepartmentEditForm;
import com.example.clinic.web.form.DepartmentForm;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Administrator controller for managing medical departments scoped to authorized branches (FR-028, TC-028).
 */
@Controller
@RequestMapping("/admin")
public class AdminDepartmentController {

    private final OrganizationAdminService organizationAdminService;

    public AdminDepartmentController(OrganizationAdminService organizationAdminService) {
        this.organizationAdminService = organizationAdminService;
    }

    @GetMapping("/departments")
    public String listDepartments(@RequestParam(required = false) UUID branchId, Model model) {
        List<BranchResponse> branches = organizationAdminService.getAuthorizedBranches();
        model.addAttribute("branches", branches);

        UUID selectedBranchId = branchId;
        List<DepartmentResponse> departments;
        if (selectedBranchId != null) {
            departments = organizationAdminService.getDepartmentsByBranch(selectedBranchId);
        } else if (!branches.isEmpty()) {
            selectedBranchId = branches.get(0).id();
            departments = organizationAdminService.getDepartmentsByBranch(selectedBranchId);
        } else {
            departments = Collections.emptyList();
        }

        model.addAttribute("selectedBranchId", selectedBranchId);
        model.addAttribute("departments", departments);

        if (!model.containsAttribute("departmentForm")) {
            DepartmentForm form = new DepartmentForm();
            form.setBranchId(selectedBranchId);
            model.addAttribute("departmentForm", form);
        }

        return "admin/departments";
    }

    @PostMapping("/departments")
    public String createDepartment(@Valid @ModelAttribute("departmentForm") DepartmentForm form,
                                   BindingResult bindingResult,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateDepartmentsOnError(form.getBranchId(), model);
            return "admin/departments";
        }

        try {
            organizationAdminService.createDepartment(form.getBranchId(), form.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Bölüm başarıyla eklendi.");
            return "redirect:/admin/departments?branchId=" + form.getBranchId();
        } catch (IllegalArgumentException | IllegalStateException e) {
            populateDepartmentsOnError(form.getBranchId(), model);
            model.addAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "Bölüm eklenemedi."));
            return "admin/departments";
        }
    }

    @PostMapping("/departments/{id}/edit")
    public String updateDepartment(@PathVariable UUID id,
                                   @RequestParam(required = false) UUID branchId,
                                   @Valid @ModelAttribute("departmentEditForm") DepartmentEditForm form,
                                   BindingResult bindingResult,
                                   RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("errorMessage", msg != null ? msg : "Bölüm adı geçersiz.");
            return redirectToBranchDepartments(branchId);
        }

        try {
            DepartmentResponse updated = organizationAdminService.updateDepartment(id, form.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Bölüm adı başarıyla güncellendi.");
            return redirectToBranchDepartments(updated.branchId() != null ? updated.branchId() : branchId);
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "Bölüm güncellenemedi."));
            return redirectToBranchDepartments(branchId);
        }
    }

    @PostMapping("/departments/{id}/activate")
    public String activateDepartment(@PathVariable UUID id,
                                     @RequestParam(required = false) UUID branchId,
                                     RedirectAttributes redirectAttributes) {
        try {
            organizationAdminService.activateDepartment(id);
            redirectAttributes.addFlashAttribute("successMessage", "Bölüm aktif duruma getirildi.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "Bölüm aktif edilemedi."));
        }
        return redirectToBranchDepartments(branchId);
    }

    @PostMapping("/departments/{id}/deactivate")
    public String deactivateDepartment(@PathVariable UUID id,
                                       @RequestParam(required = false) UUID branchId,
                                       RedirectAttributes redirectAttributes) {
        try {
            organizationAdminService.deactivateDepartment(id);
            redirectAttributes.addFlashAttribute("successMessage", "Bölüm pasif duruma alındı.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "Bölüm pasife alınamadı."));
        }
        return redirectToBranchDepartments(branchId);
    }

    private void populateDepartmentsOnError(UUID branchId, Model model) {
        List<BranchResponse> branches = organizationAdminService.getAuthorizedBranches();
        model.addAttribute("branches", branches);
        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("departments", branchId != null
                ? organizationAdminService.getDepartmentsByBranch(branchId)
                : Collections.emptyList());
    }

    private String redirectToBranchDepartments(UUID branchId) {
        return branchId != null ? "redirect:/admin/departments?branchId=" + branchId : "redirect:/admin/departments";
    }
}
