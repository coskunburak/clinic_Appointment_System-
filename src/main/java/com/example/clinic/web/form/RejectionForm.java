package com.example.clinic.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form object capturing appointment rejection reason from staff.
 */
public class RejectionForm {

    @NotBlank(message = "Red gerekçesi boş bırakılamaz.")
    @Size(min = 1, max = 500, message = "Red gerekçesi en fazla 500 karakter olabilir.")
    private String reason;

    public RejectionForm() {
    }

    public RejectionForm(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
