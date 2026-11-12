package com.example.clinic.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Form object capturing department creation parameters.
 */
public class DepartmentForm {

    @NotNull(message = "Lütfen bir şube seçiniz.")
    private UUID branchId;

    @NotBlank(message = "Bölüm adı boş bırakılamaz.")
    @Size(max = 100, message = "Bölüm adı en fazla 100 karakter olabilir.")
    private String name;

    public DepartmentForm() {
    }

    public DepartmentForm(UUID branchId, String name) {
        this.branchId = branchId;
        this.name = name;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
