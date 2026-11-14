package com.example.clinic.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form object capturing organization profile updates by authorized administrators.
 */
public class OrganizationUpdateForm {

    @NotBlank(message = "Kurum adı boş bırakılamaz.")
    @Size(max = 150, message = "Kurum adı en fazla 150 karakter olabilir.")
    private String name;

    public OrganizationUpdateForm() {
    }

    public OrganizationUpdateForm(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
