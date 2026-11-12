package com.example.clinic.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form object capturing appointment cancellation reason.
 */
public class CancellationForm {

    @NotBlank(message = "İptal gerekçesi boş bırakılamaz.")
    @Size(min = 1, max = 500, message = "İptal gerekçesi en fazla 500 karakter olabilir.")
    private String reason;

    public CancellationForm() {
    }

    public CancellationForm(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
