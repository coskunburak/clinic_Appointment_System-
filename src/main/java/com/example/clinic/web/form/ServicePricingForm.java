package com.example.clinic.web.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Form object capturing inline branch service pricing updates.
 */
public class ServicePricingForm {

    @NotNull(message = "Hizmet ücreti zorunludur.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Ücret negatif olamaz.")
    private BigDecimal fee;

    public ServicePricingForm() {
    }

    public ServicePricingForm(BigDecimal fee) {
        this.fee = fee;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }
}
