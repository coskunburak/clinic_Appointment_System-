package com.example.clinic.web.admin;

import com.example.clinic.catalog.application.CatalogManagementService;
import com.example.clinic.catalog.application.dto.BranchServiceResponse;
import com.example.clinic.catalog.application.dto.CreateServiceCommand;
import com.example.clinic.catalog.application.dto.ServiceDefinitionResponse;
import com.example.clinic.organization.application.OrganizationAdminService;
import com.example.clinic.organization.application.dto.BranchResponse;
import com.example.clinic.organization.application.dto.CreateBranchCommand;
import com.example.clinic.organization.application.dto.DepartmentResponse;
import com.example.clinic.organization.application.dto.OpeningHoursDto;
import com.example.clinic.organization.application.dto.OrganizationResponse;
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
import com.example.clinic.web.form.BranchForm;
import com.example.clinic.web.form.BranchHoursForm;
import com.example.clinic.web.form.BranchServiceForm;
import com.example.clinic.web.form.DepartmentForm;
import com.example.clinic.web.form.ServiceDefinitionForm;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Production-level Spring MVC administrator management controller (WP-S08-04, FR-028, TC-028).
 * Coordinates branches, opening hours, medical departments, service catalogue definitions,
 * personnel registration, assignments, and availability rules under strict multi-tenant authorization.
 */
@Controller
@RequestMapping("/admin")
public class AdminManagementController {

    private final OrganizationAdminService organizationAdminService;
    private final CatalogManagementService catalogManagementService;
    private final StaffManagementService staffManagementService;

    public AdminManagementController(OrganizationAdminService organizationAdminService,
                                     CatalogManagementService catalogManagementService,
                                     StaffManagementService staffManagementService) {
        this.organizationAdminService = Objects.requireNonNull(organizationAdminService);
        this.catalogManagementService = Objects.requireNonNull(catalogManagementService);
        this.staffManagementService = Objects.requireNonNull(staffManagementService);
    }

    // =========================================================================
    // 1. Dashboard (WP-S08-04, Section 9.1)
    // =========================================================================

    @GetMapping({"", "/dashboard"})
    public String dashboard(Model model) {
        List<OrganizationResponse> orgs = organizationAdminService.getAuthorizedOrganizations();
        List<BranchResponse> branches = organizationAdminService.getAuthorizedBranches();
        List<StaffProfileResponse> staff = staffManagementService.getAllStaffProfiles();

        int totalDepartments = 0;
        int totalServices = 0;
        for (BranchResponse b : branches) {
            try {
                totalDepartments += organizationAdminService.getDepartmentsByBranch(b.id()).size();
                totalServices += catalogManagementService.getBranchServicesByBranch(b.id()).size();
            } catch (Exception ignored) {
            }
        }

        model.addAttribute("organizationCount", orgs.size());
        model.addAttribute("branchCount", branches.size());
        model.addAttribute("departmentCount", totalDepartments);
        model.addAttribute("serviceCount", totalServices);
        model.addAttribute("staffCount", staff.size());
        model.addAttribute("organizations", orgs);
        model.addAttribute("branches", branches);

        return "admin/dashboard";
    }

    // =========================================================================
    // 2. Branch Management (WP-S08-04, Section 9.2)
    // =========================================================================

    @GetMapping("/branches")
    public String listBranches(Model model) {
        List<OrganizationResponse> organizations = organizationAdminService.getAuthorizedOrganizations();
        List<BranchResponse> branches = organizationAdminService.getAuthorizedBranches();

        model.addAttribute("organizations", organizations);
        model.addAttribute("branches", branches);

        if (!model.containsAttribute("branchForm")) {
            BranchForm form = new BranchForm();
            if (!organizations.isEmpty()) {
                form.setOrganizationId(organizations.get(0).id());
            }
            model.addAttribute("branchForm", form);
        }

        return "admin/branches";
    }

    @PostMapping("/branches")
    public String createBranch(@Valid @ModelAttribute("branchForm") BranchForm form,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("organizations", organizationAdminService.getAuthorizedOrganizations());
            model.addAttribute("branches", organizationAdminService.getAuthorizedBranches());
            return "admin/branches";
        }

        try {
            CreateBranchCommand command = new CreateBranchCommand(form.getName(), form.getTimeZone(), form.getAddress());
            organizationAdminService.createBranch(form.getOrganizationId(), command);
            redirectAttributes.addFlashAttribute("successMessage", "Şube başarıyla oluşturuldu: " + form.getName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Şube oluşturulamadı: " + e.getMessage());
        }

        return "redirect:/admin/branches";
    }

    @PostMapping("/branches/{id}/activate")
    public String activateBranch(@PathVariable("id") UUID id, RedirectAttributes redirectAttributes) {
        try {
            organizationAdminService.activateBranch(id);
            redirectAttributes.addFlashAttribute("successMessage", "Şube başarıyla aktifleştirildi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "İşlem başarısız: " + e.getMessage());
        }
        return "redirect:/admin/branches";
    }

    @PostMapping("/branches/{id}/deactivate")
    public String deactivateBranch(@PathVariable("id") UUID id, RedirectAttributes redirectAttributes) {
        try {
            organizationAdminService.deactivateBranch(id);
            redirectAttributes.addFlashAttribute("successMessage", "Şube devre dışı bırakıldı.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "İşlem başarısız: " + e.getMessage());
        }
        return "redirect:/admin/branches";
    }

    // =========================================================================
    // 3. Branch Working Hours (WP-S08-04, Section 9.2, FR-003)
    // =========================================================================

    @GetMapping("/branches/{id}/hours")
    public String showBranchHours(@PathVariable("id") UUID id, Model model) {
        BranchResponse branch = organizationAdminService.getBranch(id);
        List<OpeningHoursDto> hours = organizationAdminService.getOpeningHoursByBranch(id);

        model.addAttribute("branch", branch);
        model.addAttribute("openingHours", hours);

        if (!model.containsAttribute("hoursForm")) {
            BranchHoursForm form = new BranchHoursForm();
            form.setBranchId(id);
            model.addAttribute("hoursForm", form);
        }

        return "admin/branch-hours";
    }

    @PostMapping("/branches/{id}/hours")
    public String configureBranchHours(@PathVariable("id") UUID id,
                                       @Valid @ModelAttribute("hoursForm") BranchHoursForm form,
                                       BindingResult bindingResult,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("branch", organizationAdminService.getBranch(id));
            model.addAttribute("openingHours", organizationAdminService.getOpeningHoursByBranch(id));
            return "admin/branch-hours";
        }

        try {
            List<OpeningHoursDto> current = new ArrayList<>(organizationAdminService.getOpeningHoursByBranch(id));
            current.removeIf(h -> h.weekday() == form.getWeekday());
            current.add(new OpeningHoursDto(form.getWeekday(), form.getStartTime(), form.getEndTime()));

            organizationAdminService.configureOpeningHours(id, current);
            redirectAttributes.addFlashAttribute("successMessage", "Çalışma saatleri başarıyla kaydedildi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Çalışma saati kaydedilemedi: " + e.getMessage());
        }

        return "redirect:/admin/branches/" + id + "/hours";
    }

    // =========================================================================
    // 4. Department Management (WP-S08-04, Section 9.3, FR-004)
    // =========================================================================

    @GetMapping("/departments")
    public String listDepartments(@RequestParam(value = "branchId", required = false) UUID branchId, Model model) {
        List<BranchResponse> branches = organizationAdminService.getAuthorizedBranches();
        model.addAttribute("branches", branches);

        UUID selectedBranchId = branchId;
        if (selectedBranchId == null && !branches.isEmpty()) {
            selectedBranchId = branches.get(0).id();
        }
        model.addAttribute("selectedBranchId", selectedBranchId);

        List<DepartmentResponse> departments = Collections.emptyList();
        if (selectedBranchId != null) {
            try {
                departments = organizationAdminService.getDepartmentsByBranch(selectedBranchId);
            } catch (Exception ignored) {
            }
        }
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
            List<BranchResponse> branches = organizationAdminService.getAuthorizedBranches();
            model.addAttribute("branches", branches);
            model.addAttribute("selectedBranchId", form.getBranchId());
            model.addAttribute("departments", form.getBranchId() != null
                    ? organizationAdminService.getDepartmentsByBranch(form.getBranchId()) : Collections.emptyList());
            return "admin/departments";
        }

        try {
            organizationAdminService.createDepartment(form.getBranchId(), form.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Bölüm başarıyla oluşturuldu: " + form.getName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bölüm oluşturulamadı: " + e.getMessage());
        }

        return "redirect:/admin/departments?branchId=" + form.getBranchId();
    }

    // =========================================================================
    // 5. Medical Services Management (WP-S08-04, Section 9.4, FR-005, FR-006)
    // =========================================================================

    @GetMapping("/services")
    public String listServices(Model model) {
        List<OrganizationResponse> organizations = organizationAdminService.getAuthorizedOrganizations();
        List<BranchResponse> branches = organizationAdminService.getAuthorizedBranches();

        List<ServiceDefinitionResponse> definitions = new ArrayList<>();
        for (OrganizationResponse org : organizations) {
            try {
                definitions.addAll(catalogManagementService.getServiceDefinitionsByOrganization(org.id()));
            } catch (Exception ignored) {
            }
        }

        List<BranchServiceResponse> branchServices = new ArrayList<>();
        for (BranchResponse branch : branches) {
            try {
                branchServices.addAll(catalogManagementService.getBranchServicesByBranch(branch.id()));
            } catch (Exception ignored) {
            }
        }

        model.addAttribute("organizations", organizations);
        model.addAttribute("branches", branches);
        model.addAttribute("serviceDefinitions", definitions);
        model.addAttribute("branchServices", branchServices);

        if (!model.containsAttribute("serviceDefinitionForm")) {
            ServiceDefinitionForm defForm = new ServiceDefinitionForm();
            if (!organizations.isEmpty()) {
                defForm.setOrganizationId(organizations.get(0).id());
            }
            model.addAttribute("serviceDefinitionForm", defForm);
        }

        if (!model.containsAttribute("branchServiceForm")) {
            BranchServiceForm bsForm = new BranchServiceForm();
            if (!branches.isEmpty()) {
                bsForm.setBranchId(branches.get(0).id());
            }
            if (!definitions.isEmpty()) {
                bsForm.setServiceDefinitionId(definitions.get(0).id());
            }
            model.addAttribute("branchServiceForm", bsForm);
        }

        return "admin/services";
    }

    @PostMapping("/services/definitions")
    public String createServiceDefinition(@Valid @ModelAttribute("serviceDefinitionForm") ServiceDefinitionForm form,
                                          BindingResult bindingResult,
                                          Model model,
                                          RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return listServices(model);
        }

        try {
            CreateServiceCommand command = new CreateServiceCommand(form.getName(), form.getDescription());
            catalogManagementService.defineService(form.getOrganizationId(), command);
            redirectAttributes.addFlashAttribute("successMessage", "Hizmet tanımı başarıyla oluşturuldu: " + form.getName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hizmet tanımı oluşturulamadı: " + e.getMessage());
        }

        return "redirect:/admin/services";
    }

    @PostMapping("/services/assign")
    public String assignServiceToBranch(@Valid @ModelAttribute("branchServiceForm") BranchServiceForm form,
                                        BindingResult bindingResult,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return listServices(model);
        }

        try {
            catalogManagementService.assignServiceToBranch(
                    form.getBranchId(),
                    form.getServiceDefinitionId(),
                    form.getDurationMinutes(),
                    form.getFee(),
                    form.getCurrency()
            );
            redirectAttributes.addFlashAttribute("successMessage", "Hizmet şubeye başarıyla atandı ve fiyatlandırıldı.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hizmet atanamadı: " + e.getMessage());
        }

        return "redirect:/admin/services";
    }

    @PostMapping("/services/{id}/pricing")
    public String updateServicePricing(@PathVariable("id") UUID id,
                                       @RequestParam("fee") BigDecimal fee,
                                       RedirectAttributes redirectAttributes) {
        try {
            catalogManagementService.updateServicePricing(id, fee);
            redirectAttributes.addFlashAttribute("successMessage", "Hizmet ücreti güncellendi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ücret güncellenemedi: " + e.getMessage());
        }
        return "redirect:/admin/services";
    }

    @PostMapping("/services/{id}/duration")
    public String updateServiceDuration(@PathVariable("id") UUID id,
                                        @RequestParam("durationMinutes") int durationMinutes,
                                        RedirectAttributes redirectAttributes) {
        try {
            catalogManagementService.updateServiceDuration(id, durationMinutes);
            redirectAttributes.addFlashAttribute("successMessage", "Hizmet süresi güncellendi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Süre güncellenemedi: " + e.getMessage());
        }
        return "redirect:/admin/services";
    }

    @PostMapping("/services/{id}/activate")
    public String activateBranchService(@PathVariable("id") UUID id, RedirectAttributes redirectAttributes) {
        try {
            catalogManagementService.activateBranchService(id);
            redirectAttributes.addFlashAttribute("successMessage", "Şube hizmeti aktifleştirildi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "İşlem başarısız: " + e.getMessage());
        }
        return "redirect:/admin/services";
    }

    @PostMapping("/services/{id}/deactivate")
    public String deactivateBranchService(@PathVariable("id") UUID id, RedirectAttributes redirectAttributes) {
        try {
            catalogManagementService.deactivateBranchService(id);
            redirectAttributes.addFlashAttribute("successMessage", "Şube hizmeti devre dışı bırakıldı.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "İşlem başarısız: " + e.getMessage());
        }
        return "redirect:/admin/services";
    }

    // =========================================================================
    // 6. Personnel & Availability Management (WP-S08-04, Section 9.5, 9.6)
    // =========================================================================

    @GetMapping("/staff")
    public String listStaff(Model model) {
        List<BranchResponse> branches = organizationAdminService.getAuthorizedBranches();
        List<StaffProfileResponse> staffProfiles = staffManagementService.getAllStaffProfiles();
        List<StaffAssignmentResponse> assignments = staffManagementService.getAllStaffAssignments();

        List<DepartmentResponse> departments = new ArrayList<>();
        List<BranchServiceResponse> branchServices = new ArrayList<>();
        for (BranchResponse b : branches) {
            try {
                departments.addAll(organizationAdminService.getDepartmentsByBranch(b.id()));
                branchServices.addAll(catalogManagementService.getBranchServicesByBranch(b.id()));
            } catch (Exception ignored) {
            }
        }

        model.addAttribute("branches", branches);
        model.addAttribute("departments", departments);
        model.addAttribute("staffProfiles", staffProfiles);
        model.addAttribute("assignments", assignments);
        model.addAttribute("branchServices", branchServices);

        if (!model.containsAttribute("staffForm")) {
            StaffRegistrationForm form = new StaffRegistrationForm();
            if (!branches.isEmpty()) {
                form.setBranchId(branches.get(0).id());
            }
            model.addAttribute("staffForm", form);
        }

        if (!model.containsAttribute("assignmentForm")) {
            StaffAssignmentForm aForm = new StaffAssignmentForm();
            if (!staffProfiles.isEmpty()) {
                aForm.setStaffId(staffProfiles.get(0).id());
            }
            if (!branches.isEmpty()) {
                aForm.setBranchId(branches.get(0).id());
            }
            if (!departments.isEmpty()) {
                aForm.setDepartmentId(departments.get(0).id());
            }
            model.addAttribute("assignmentForm", aForm);
        }

        if (!model.containsAttribute("eligibilityForm")) {
            StaffEligibilityForm eForm = new StaffEligibilityForm();
            if (!assignments.isEmpty()) {
                eForm.setAssignmentId(assignments.get(0).id());
            }
            model.addAttribute("eligibilityForm", eForm);
        }

        return "admin/staff";
    }

    @PostMapping("/staff")
    public String registerStaff(@Valid @ModelAttribute("staffForm") StaffRegistrationForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return listStaff(model);
        }

        try {
            CreateStaffCommand command = new CreateStaffCommand(
                    form.getBranchId(),
                    form.getEmail(),
                    form.getPassword(),
                    form.getFullName(),
                    form.getPhone(),
                    form.getStaffType(),
                    form.getSpecialty()
            );
            staffManagementService.registerStaff(command);
            redirectAttributes.addFlashAttribute("successMessage", "Personel hesabı ve profili başarıyla kaydedildi: " + form.getFullName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Personel kaydedilemedi: " + e.getMessage());
        }

        return "redirect:/admin/staff";
    }

    @PostMapping("/staff/assign")
    public String assignStaff(@Valid @ModelAttribute("assignmentForm") StaffAssignmentForm form,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return listStaff(model);
        }

        try {
            AssignStaffCommand command = new AssignStaffCommand(form.getStaffId(), form.getBranchId(), form.getDepartmentId());
            staffManagementService.assignStaffToBranch(command);
            redirectAttributes.addFlashAttribute("successMessage", "Personel şube ve bölüme başarıyla görevlendirildi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Görevlendirme başarısız: " + e.getMessage());
        }

        return "redirect:/admin/staff";
    }

    @PostMapping("/staff/eligibility")
    public String configureEligibility(@Valid @ModelAttribute("eligibilityForm") StaffEligibilityForm form,
                                       BindingResult bindingResult,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return listStaff(model);
        }

        try {
            ConfigureEligibilityCommand command = new ConfigureEligibilityCommand(
                    form.getAssignmentId(),
                    form.getBranchServiceIds() != null ? form.getBranchServiceIds() : Collections.emptyList()
            );
            staffManagementService.configureEligibility(command);
            redirectAttributes.addFlashAttribute("successMessage", "Hekim hizmet yetkileri başarıyla güncellendi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hizmet yetkilendirmesi başarısız: " + e.getMessage());
        }

        return "redirect:/admin/staff";
    }

    @GetMapping("/staff/{id}/availability")
    public String showStaffAvailability(@PathVariable("id") UUID staffId, Model model) {
        StaffProfileResponse staff = staffManagementService.getStaffProfile(staffId);
        List<StaffAssignmentResponse> assignments = staffManagementService.getStaffAssignmentsByStaff(staffId);

        model.addAttribute("staff", staff);
        model.addAttribute("assignments", assignments);

        if (!assignments.isEmpty()) {
            UUID assignmentId = assignments.get(0).id();
            model.addAttribute("selectedAssignmentId", assignmentId);
            model.addAttribute("weeklyAvailabilities", staffManagementService.getWeeklyAvailabilitiesByAssignment(assignmentId));
            model.addAttribute("exceptions", staffManagementService.getAvailabilityExceptionsByAssignment(assignmentId));

            WeeklyAvailabilityForm wForm = new WeeklyAvailabilityForm();
            wForm.setAssignmentId(assignmentId);
            model.addAttribute("weeklyForm", wForm);

            StaffExceptionForm eForm = new StaffExceptionForm();
            eForm.setAssignmentId(assignmentId);
            model.addAttribute("exceptionForm", eForm);
        }

        return "admin/staff-availability";
    }

    @PostMapping("/staff/assignments/{assignmentId}/weekly-availability")
    public String addWeeklyAvailability(@PathVariable("assignmentId") UUID assignmentId,
                                        @Valid @ModelAttribute("weeklyForm") WeeklyAvailabilityForm form,
                                        BindingResult bindingResult,
                                        RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Haftalık mesai parametreleri geçersiz.");
            return "redirect:/admin/staff";
        }

        try {
            List<WeeklyAvailabilityResponse> current = staffManagementService.getWeeklyAvailabilitiesByAssignment(assignmentId);
            List<WeeklyAvailabilityItem> items = new ArrayList<>();
            for (WeeklyAvailabilityResponse resp : current) {
                items.add(new WeeklyAvailabilityItem(resp.weekday(), resp.startTime(), resp.endTime()));
            }
            items.add(new WeeklyAvailabilityItem(form.getWeekday(), form.getStartTime(), form.getEndTime()));

            staffManagementService.configureWeeklyAvailability(new ConfigureWeeklyAvailabilityCommand(assignmentId, items));
            redirectAttributes.addFlashAttribute("successMessage", "Haftalık uygunluk mesaisi eklendi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mesai eklenemedi: " + e.getMessage());
        }

        return "redirect:/admin/staff";
    }

    @PostMapping("/staff/assignments/{assignmentId}/exceptions")
    public String addException(@PathVariable("assignmentId") UUID assignmentId,
                               @Valid @ModelAttribute("exceptionForm") StaffExceptionForm form,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "İstisna parametreleri geçersiz.");
            return "redirect:/admin/staff";
        }

        try {
            CreateAvailabilityExceptionCommand command = new CreateAvailabilityExceptionCommand(
                    assignmentId,
                    form.getLocalDate(),
                    form.getKind(),
                    form.getStartTime(),
                    form.getEndTime(),
                    form.getReason()
            );
            staffManagementService.addAvailabilityException(command);
            redirectAttributes.addFlashAttribute("successMessage", "İzin / mesai istisnası kaydedildi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "İstisna kaydedilemedi: " + e.getMessage());
        }

        return "redirect:/admin/staff";
    }
}
