package com.example.clinic.web.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Form object capturing branch service assignment and pricing.
 */
public class BranchServiceForm {

    @NotNull(message = "Lütfen bir şube seçiniz.")
    private UUID branchId;

    @NotNull(message = "Lütfen bir hizmet tanımı seçiniz.")
    private UUID serviceDefinitionId;

    @NotNull(message = "Hizmet süresi zorunludur.")
    @Positive(message = "Hizmet süresi pozitif bir tam sayı olmalıdır.")
    private Integer durationMinutes = 30;

    @NotNull(message = "Hizmet ücreti zorunludur.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Ücret negatif olamaz.")
    private BigDecimal fee = BigDecimal.valueOf(500.00);

    @NotBlank(message = "Para birimi zorunludur.")
    @Size(min = 3, max = 3, message = "Para birimi 3 karakterli ISO kodu olmalıdır.")
    private String currency = "TRY";

    public BranchServiceForm() {
    }

    public BranchServiceForm(UUID branchId, UUID serviceDefinitionId, Integer durationMinutes, BigDecimal fee, String currency) {
        this.branchId = branchId;
        this.serviceDefinitionId = serviceDefinitionId;
        this.durationMinutes = durationMinutes;
        this.fee = fee;
        this.currency = currency;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public UUID getServiceDefinitionId() {
        return serviceDefinitionId;
    }

    public void setServiceDefinitionId(UUID serviceDefinitionId) {
        this.serviceDefinitionId = serviceDefinitionId;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
