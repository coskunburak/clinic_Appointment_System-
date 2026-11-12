package com.example.clinic.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Form object capturing organization-level service definition creation.
 */
public class ServiceDefinitionForm {

    @NotNull(message = "Lütfen bir kurum seçiniz.")
    private UUID organizationId;

    @NotBlank(message = "Hizmet adı boş bırakılamaz.")
    @Size(max = 100, message = "Hizmet adı en fazla 100 karakter olabilir.")
    private String name;

    @Size(max = 1000, message = "Açıklama en fazla 1000 karakter olabilir.")
    private String description;

    public ServiceDefinitionForm() {
    }

    public ServiceDefinitionForm(UUID organizationId, String name, String description) {
        this.organizationId = organizationId;
        this.name = name;
        this.description = description;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
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
