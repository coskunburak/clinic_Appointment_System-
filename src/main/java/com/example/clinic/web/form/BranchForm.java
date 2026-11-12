package com.example.clinic.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Form object capturing branch creation parameters.
 */
public class BranchForm {

    @NotNull(message = "Lütfen bir kurum seçiniz.")
    private UUID organizationId;

    @NotBlank(message = "Şube adı boş bırakılamaz.")
    @Size(max = 100, message = "Şube adı en fazla 100 karakter olabilir.")
    private String name;

    @NotBlank(message = "Zaman dilimi boş bırakılamaz.")
    private String timeZone = "Europe/Istanbul";

    @Size(max = 500, message = "Adres en fazla 500 karakter olabilir.")
    private String address;

    public BranchForm() {
    }

    public BranchForm(UUID organizationId, String name, String timeZone, String address) {
        this.organizationId = organizationId;
        this.name = name;
        this.timeZone = timeZone;
        this.address = address;
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

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
