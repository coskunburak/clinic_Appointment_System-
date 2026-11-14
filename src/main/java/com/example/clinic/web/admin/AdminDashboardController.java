package com.example.clinic.web.admin;

import com.example.clinic.catalog.application.CatalogManagementService;
import com.example.clinic.organization.application.OrganizationAdminService;
import com.example.clinic.organization.application.dto.BranchResponse;
import com.example.clinic.organization.application.dto.OrganizationResponse;
import com.example.clinic.staff.application.StaffManagementService;
import com.example.clinic.staff.application.dto.StaffProfileResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Administrator landing dashboard controller summarizing authorized organization, branch,
 * catalog, and personnel metrics under tenant-scoped visibility (FR-028, TC-028).
 */
@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final OrganizationAdminService organizationAdminService;
    private final CatalogManagementService catalogManagementService;
    private final StaffManagementService staffManagementService;

    public AdminDashboardController(OrganizationAdminService organizationAdminService,
                                    CatalogManagementService catalogManagementService,
                                    StaffManagementService staffManagementService) {
        this.organizationAdminService = organizationAdminService;
        this.catalogManagementService = catalogManagementService;
        this.staffManagementService = staffManagementService;
    }

    @GetMapping({"", "/dashboard"})
    public String dashboard(Model model) {
        List<OrganizationResponse> organizations = organizationAdminService.getAuthorizedOrganizations();
        List<OrganizationResponse> orgWideOrganizations = organizationAdminService.getOrgWideAuthorizedOrganizations();
        List<BranchResponse> branches = organizationAdminService.getAuthorizedBranches();
        List<StaffProfileResponse> staff = staffManagementService.getAuthorizedStaffProfiles();

        int totalDepartments = 0;
        int totalServices = 0;
        for (BranchResponse branch : branches) {
            totalDepartments += organizationAdminService.getDepartmentsByBranch(branch.id()).size();
            totalServices += catalogManagementService.getBranchServicesByBranch(branch.id()).size();
        }

        model.addAttribute("organizationCount", organizations.size());
        model.addAttribute("orgWideOrganizationCount", orgWideOrganizations.size());
        model.addAttribute("branchCount", branches.size());
        model.addAttribute("departmentCount", totalDepartments);
        model.addAttribute("serviceCount", totalServices);
        model.addAttribute("staffCount", staff.size());
        model.addAttribute("organizations", organizations);
        model.addAttribute("orgWideOrganizations", orgWideOrganizations);
        model.addAttribute("branches", branches);

        return "admin/dashboard";
    }
}
