package com.example.clinic.web.form;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Form object capturing staff assignment to branch and department.
 */
public class StaffAssignmentForm {

    @NotNull(message = "Lütfen bir personel seçiniz.")
    private UUID staffId;

    @NotNull(message = "Lütfen bir şube seçiniz.")
    private UUID branchId;

    @NotNull(message = "Lütfen bir bölüm seçiniz.")
    private UUID departmentId;

    public StaffAssignmentForm() {
    }

    public StaffAssignmentForm(UUID staffId, UUID branchId, UUID departmentId) {
        this.staffId = staffId;
        this.branchId = branchId;
        this.departmentId = departmentId;
    }

    public UUID getStaffId() {
        return staffId;
    }

    public void setStaffId(UUID staffId) {
        this.staffId = staffId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public UUID getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(UUID departmentId) {
        this.departmentId = departmentId;
    }
}
