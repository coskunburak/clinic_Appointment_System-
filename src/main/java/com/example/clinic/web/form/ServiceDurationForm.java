package com.example.clinic.web.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Form object capturing inline branch service duration updates.
 */
public class ServiceDurationForm {

    @NotNull(message = "Hizmet süresi zorunludur.")
    @Positive(message = "Hizmet süresi pozitif bir tam sayı olmalıdır.")
    private Integer durationMinutes;

    public ServiceDurationForm() {
    }

    public ServiceDurationForm(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}
