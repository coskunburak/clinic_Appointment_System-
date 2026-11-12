package com.example.clinic.web.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Form object capturing a single weekday availability interval for staff assignment.
 */
public class WeeklyAvailabilityForm {

    @NotNull(message = "Lütfen bir personel görevlendirmesi seçiniz.")
    private UUID assignmentId;

    @NotNull(message = "Haftanın günü zorunludur.")
    @Min(value = 1, message = "Gün 1 (Pazartesi) ile 7 (Pazar) arasında olmalıdır.")
    @Max(value = 7, message = "Gün 1 (Pazartesi) ile 7 (Pazar) arasında olmalıdır.")
    private Short weekday = 1;

    @NotNull(message = "Başlangıç saati zorunludur.")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime = LocalTime.of(9, 0);

    @NotNull(message = "Bitiş saati zorunludur.")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime = LocalTime.of(17, 0);

    public WeeklyAvailabilityForm() {
    }

    public WeeklyAvailabilityForm(UUID assignmentId, Short weekday, LocalTime startTime, LocalTime endTime) {
        this.assignmentId = assignmentId;
        this.weekday = weekday;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public UUID getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(UUID assignmentId) {
        this.assignmentId = assignmentId;
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
