package com.example.clinic.web.form;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Form object capturing patient booking submission (WP-S08-02).
 */
public class BookingForm {

    @NotNull(message = "Lütfen bir şube seçiniz.")
    private UUID branchId;

    @NotNull(message = "Lütfen bir tıbbi hizmet seçiniz.")
    private UUID branchServiceId;

    @NotNull(message = "Lütfen bir hekim seçiniz.")
    private UUID staffId;

    @NotNull(message = "Lütfen bir randevu tarihi seçiniz.")
    @FutureOrPresent(message = "Randevu tarihi geçmiş bir tarih olamaz.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate appointmentDate;

    @NotNull(message = "Lütfen bir randevu saati seçiniz.")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    public BookingForm() {
    }

    public BookingForm(UUID branchId, UUID branchServiceId, UUID staffId, LocalDate appointmentDate, LocalTime startTime) {
        this.branchId = branchId;
        this.branchServiceId = branchServiceId;
        this.staffId = staffId;
        this.appointmentDate = appointmentDate;
        this.startTime = startTime;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public UUID getBranchServiceId() {
        return branchServiceId;
    }

    public void setBranchServiceId(UUID branchServiceId) {
        this.branchServiceId = branchServiceId;
    }

    public UUID getStaffId() {
        return staffId;
    }

    public void setStaffId(UUID staffId) {
        this.staffId = staffId;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }
}
