package com.example.clinic.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form object capturing medical department updates by authorized administrators.
 */
public class DepartmentEditForm {

    @NotBlank(message = "Bölüm adı boş bırakılamaz.")
    @Size(max = 100, message = "Bölüm adı en fazla 100 karakter olabilir.")
    private String name;

    public DepartmentEditForm() {
    }

    public DepartmentEditForm(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
