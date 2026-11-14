package com.example.clinic.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form object capturing existing branch edits by authorized administrators.
 */
public class BranchEditForm {

    @NotBlank(message = "Şube adı boş bırakılamaz.")
    @Size(max = 100, message = "Şube adı en fazla 100 karakter olabilir.")
    private String name;

    @NotBlank(message = "Zaman dilimi boş bırakılamaz.")
    private String timeZone = "Europe/Istanbul";

    @Size(max = 500, message = "Adres en fazla 500 karakter olabilir.")
    private String address;

    public BranchEditForm() {
    }

    public BranchEditForm(String name, String timeZone, String address) {
        this.name = name;
        this.timeZone = timeZone;
        this.address = address;
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
