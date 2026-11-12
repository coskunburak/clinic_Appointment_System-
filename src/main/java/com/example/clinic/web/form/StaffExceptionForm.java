package com.example.clinic.web.form;

import com.example.clinic.staff.domain.AvailabilityExceptionKind;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Form object capturing a staff availability exception (leave or overtime).
 */
public class StaffExceptionForm {

    @NotNull(message = "Lütfen bir personel görevlendirmesi seçiniz.")
    private UUID assignmentId;

    @NotNull(message = "İstisna tarihi zorunludur.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate localDate;

    @NotNull(message = "İstisna türü zorunludur.")
    private AvailabilityExceptionKind kind = AvailabilityExceptionKind.ANNUAL_LEAVE;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @Size(max = 255, message = "Açıklama en fazla 255 karakter olabilir.")
    private String reason;

    public StaffExceptionForm() {
    }

    public StaffExceptionForm(UUID assignmentId, LocalDate localDate, AvailabilityExceptionKind kind, LocalTime startTime, LocalTime endTime, String reason) {
        this.assignmentId = assignmentId;
        this.localDate = localDate;
        this.kind = kind;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;
    }

    public UUID getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(UUID assignmentId) {
        this.assignmentId = assignmentId;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public void setLocalDate(LocalDate localDate) {
        this.localDate = localDate;
    }

    public AvailabilityExceptionKind getKind() {
        return kind;
    }

    public void setKind(AvailabilityExceptionKind kind) {
        this.kind = kind;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
