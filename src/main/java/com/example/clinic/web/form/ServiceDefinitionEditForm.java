package com.example.clinic.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form object capturing updates to organization-level service definitions.
 */
public class ServiceDefinitionEditForm {

    @NotBlank(message = "Hizmet adı boş bırakılamaz.")
    @Size(max = 100, message = "Hizmet adı en fazla 100 karakter olabilir.")
    private String name;

    @Size(max = 1000, message = "Açıklama en fazla 1000 karakter olabilir.")
    private String description;

    public ServiceDefinitionEditForm() {
    }

    public ServiceDefinitionEditForm(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
