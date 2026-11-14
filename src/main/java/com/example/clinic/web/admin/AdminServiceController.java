package com.example.clinic.web.admin;

import com.example.clinic.catalog.application.CatalogManagementService;
import com.example.clinic.catalog.application.dto.BranchServiceResponse;
import com.example.clinic.catalog.application.dto.CreateServiceCommand;
import com.example.clinic.catalog.application.dto.ServiceDefinitionResponse;
import com.example.clinic.common.exception.CurrencyMismatchException;
import com.example.clinic.organization.application.OrganizationAdminService;
import com.example.clinic.organization.application.dto.BranchResponse;
import com.example.clinic.organization.application.dto.OrganizationResponse;
import com.example.clinic.web.common.WebErrorMessageMapper;
import com.example.clinic.web.form.BranchServiceForm;
import com.example.clinic.web.form.ServiceDefinitionEditForm;
import com.example.clinic.web.form.ServiceDefinitionForm;
import com.example.clinic.web.form.ServiceDurationForm;
import com.example.clinic.web.form.ServicePricingForm;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Administrator controller for managing organization-level service definitions
 * and branch-level service assignments, duration, pricing, and lifecycle status (FR-028, TC-028).
 */
@Controller
@RequestMapping("/admin")
public class AdminServiceController {

    private final CatalogManagementService catalogManagementService;
    private final OrganizationAdminService organizationAdminService;

    public AdminServiceController(CatalogManagementService catalogManagementService,
                                  OrganizationAdminService organizationAdminService) {
        this.catalogManagementService = catalogManagementService;
        this.organizationAdminService = organizationAdminService;
    }

    @GetMapping("/services")
    public String manageServices(@RequestParam(required = false) UUID branchId, Model model) {
        populateServicesModel(model, branchId);
        if (!model.containsAttribute("serviceDefinitionForm")) {
            model.addAttribute("serviceDefinitionForm", new ServiceDefinitionForm());
        }
        if (!model.containsAttribute("branchServiceForm")) {
            BranchServiceForm bsForm = new BranchServiceForm();
            bsForm.setCurrency("TRY");
            bsForm.setBranchId(branchId);
            model.addAttribute("branchServiceForm", bsForm);
        }
        return "admin/services";
    }

    @PostMapping("/services/definitions")
    public String defineService(@Valid @ModelAttribute("serviceDefinitionForm") ServiceDefinitionForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateServicesModel(model, null);
            ensureBranchServiceForm(model);
            return "admin/services";
        }

        try {
            catalogManagementService.defineService(
                    form.getOrganizationId(),
                    new CreateServiceCommand(form.getName(), form.getDescription())
            );
            redirectAttributes.addFlashAttribute("successMessage", "Hizmet tanımı kataloğa eklendi.");
            return "redirect:/admin/services";
        } catch (IllegalArgumentException | IllegalStateException e) {
            populateServicesModel(model, null);
            ensureBranchServiceForm(model);
            model.addAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "Hizmet tanımı oluşturulamadı."));
            return "admin/services";
        }
    }

    @PostMapping("/services/definitions/{id}/edit")
    public String updateServiceDefinition(@PathVariable UUID id,
                                          @Valid @ModelAttribute("serviceDefinitionEditForm") ServiceDefinitionEditForm form,
                                          BindingResult bindingResult,
                                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("errorMessage", msg != null ? msg : "Hizmet tanımı bilgileri geçersiz.");
            return "redirect:/admin/services";
        }
        try {
            catalogManagementService.updateServiceDefinition(id, form.getName(), form.getDescription());
            redirectAttributes.addFlashAttribute("successMessage", "Hizmet tanımı güncellendi.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "Hizmet tanımı güncellenemedi."));
        }
        return "redirect:/admin/services";
    }

    @PostMapping("/services/definitions/{id}/activate")
    public String activateServiceDefinition(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            catalogManagementService.activateServiceDefinition(id);
            redirectAttributes.addFlashAttribute("successMessage", "Hizmet tanımı aktif duruma getirildi.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "Hizmet tanımı aktif edilemedi."));
        }
        return "redirect:/admin/services";
    }

    @PostMapping("/services/definitions/{id}/deactivate")
    public String deactivateServiceDefinition(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            catalogManagementService.deactivateServiceDefinition(id);
            redirectAttributes.addFlashAttribute("successMessage", "Hizmet tanımı pasif duruma alındı.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "Hizmet tanımı pasife alınamadı."));
        }
        return "redirect:/admin/services";
    }

    @PostMapping("/services/assign")
    public String assignServiceToBranch(@Valid @ModelAttribute("branchServiceForm") BranchServiceForm form,
                                        BindingResult bindingResult,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateServicesModel(model, form.getBranchId());
            ensureServiceDefinitionForm(model);
            return "admin/services";
        }

        try {
            catalogManagementService.assignServiceToBranch(
                    form.getBranchId(),
                    form.getServiceDefinitionId(),
                    form.getDurationMinutes(),
                    form.getFee(),
                    form.getCurrency()
            );
            redirectAttributes.addFlashAttribute("successMessage", "Hizmet şubeye başarıyla atandı.");
            return "redirect:/admin/services";
        } catch (IllegalArgumentException | IllegalStateException | CurrencyMismatchException e) {
            populateServicesModel(model, form.getBranchId());
            ensureServiceDefinitionForm(model);
            model.addAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "Hizmet şubeye atanamadı."));
            return "admin/services";
        }
    }

    @PostMapping("/services/{id}/pricing")
    public String updateServicePricing(@PathVariable UUID id,
                                       @Valid @ModelAttribute ServicePricingForm form,
                                       BindingResult bindingResult,
                                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ücret sıfır veya pozitif bir sayı olmalıdır.");
            return "redirect:/admin/services";
        }
        try {
            catalogManagementService.updateServicePricing(id, form.getFee());
            redirectAttributes.addFlashAttribute("successMessage", "Hizmet ücreti güncellendi.");
        } catch (IllegalArgumentException | IllegalStateException | CurrencyMismatchException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "Ücret güncellenemedi."));
        }
        return "redirect:/admin/services";
    }

    @PostMapping("/services/{id}/duration")
    public String updateServiceDuration(@PathVariable UUID id,
                                        @Valid @ModelAttribute ServiceDurationForm form,
                                        BindingResult bindingResult,
                                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hizmet süresi pozitif bir tam sayı olmalıdır.");
            return "redirect:/admin/services";
        }
        try {
            catalogManagementService.updateServiceDuration(id, form.getDurationMinutes());
            redirectAttributes.addFlashAttribute("successMessage", "Hizmet süresi güncellendi.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "Süre güncellenemedi."));
        }
        return "redirect:/admin/services";
    }

    @PostMapping("/services/{id}/activate")
    public String activateBranchService(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            catalogManagementService.activateBranchService(id);
            redirectAttributes.addFlashAttribute("successMessage", "Şube hizmeti aktif duruma getirildi.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "Şube hizmeti aktif edilemedi."));
        }
        return "redirect:/admin/services";
    }

    @PostMapping("/services/{id}/deactivate")
    public String deactivateBranchService(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            catalogManagementService.deactivateBranchService(id);
            redirectAttributes.addFlashAttribute("successMessage", "Şube hizmeti pasif duruma alındı.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    WebErrorMessageMapper.mapAdminError(e, "Şube hizmeti pasife alınamadı."));
        }
        return "redirect:/admin/services";
    }

    private void populateServicesModel(Model model, UUID branchId) {
        List<OrganizationResponse> orgWideOrgs = organizationAdminService.getOrgWideAuthorizedOrganizations();
        List<BranchResponse> branches = organizationAdminService.getAuthorizedBranches();

        List<ServiceDefinitionResponse> definitions = new ArrayList<>();
        for (OrganizationResponse org : orgWideOrgs) {
            definitions.addAll(catalogManagementService.getServiceDefinitionsByOrganization(org.id()));
        }

        List<BranchServiceResponse> branchServices = new ArrayList<>();
        if (branchId != null) {
            branchServices.addAll(catalogManagementService.getBranchServicesByBranch(branchId));
        } else {
            for (BranchResponse branch : branches) {
                branchServices.addAll(catalogManagementService.getBranchServicesByBranch(branch.id()));
            }
        }

        model.addAttribute("organizations", orgWideOrgs);
        model.addAttribute("canManageOrgCatalog", !orgWideOrgs.isEmpty());
        model.addAttribute("branches", branches);
        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("serviceDefinitions", definitions);
        model.addAttribute("branchServices", branchServices);
    }

    private void ensureServiceDefinitionForm(Model model) {
        if (!model.containsAttribute("serviceDefinitionForm")) {
            model.addAttribute("serviceDefinitionForm", new ServiceDefinitionForm());
        }
    }

    private void ensureBranchServiceForm(Model model) {
        if (!model.containsAttribute("branchServiceForm")) {
            BranchServiceForm bsForm = new BranchServiceForm();
            bsForm.setCurrency("TRY");
            model.addAttribute("branchServiceForm", bsForm);
        }
    }
}
