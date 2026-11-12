package com.example.clinic.web.form;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Form object capturing service eligibility assignment to staff assignment.
 */
public class StaffEligibilityForm {

    @NotNull(message = "Lütfen bir personel görevlendirmesi seçiniz.")
    private UUID assignmentId;

    private List<UUID> branchServiceIds = new ArrayList<>();

    public StaffEligibilityForm() {
    }

    public StaffEligibilityForm(UUID assignmentId, List<UUID> branchServiceIds) {
        this.assignmentId = assignmentId;
        this.branchServiceIds = branchServiceIds != null ? branchServiceIds : new ArrayList<>();
    }

    public UUID getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(UUID assignmentId) {
        this.assignmentId = assignmentId;
    }

    public List<UUID> getBranchServiceIds() {
        return branchServiceIds;
    }

    public void setBranchServiceIds(List<UUID> branchServiceIds) {
        this.branchServiceIds = branchServiceIds != null ? branchServiceIds : new ArrayList<>();
    }
}
