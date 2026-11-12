package com.example.clinic.web.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Form object capturing a single weekday opening hours entry.
 */
public class BranchHoursForm {

    @NotNull(message = "Lütfen bir şube seçiniz.")
    private UUID branchId;

    @NotNull(message = "Haftanın günü zorunludur.")
    @Min(value = 1, message = "Gün 1 (Pazartesi) ile 7 (Pazar) arasında olmalıdır.")
    @Max(value = 7, message = "Gün 1 (Pazartesi) ile 7 (Pazar) arasında olmalıdır.")
    private Short weekday = 1;

    @NotNull(message = "Açılış saati zorunludur.")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime = LocalTime.of(9, 0);

    @NotNull(message = "Kapanış saati zorunludur.")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime = LocalTime.of(17, 0);

    public BranchHoursForm() {
    }

    public BranchHoursForm(UUID branchId, Short weekday, LocalTime startTime, LocalTime endTime) {
        this.branchId = branchId;
        this.weekday = weekday;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public Short getWeekday() {
        return weekday;
    }

    public void setWeekday(Short weekday) {
        this.weekday = weekday;
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
}
