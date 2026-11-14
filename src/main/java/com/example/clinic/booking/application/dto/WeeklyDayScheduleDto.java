package com.example.clinic.booking.application.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Presentation DTO grouping a clinician's weekly appointments by local calendar day (Monday–Sunday).
 */
public record WeeklyDayScheduleDto(
        LocalDate date,
        String dayNameTr,
        String formattedDate,
        boolean selected,
        List<AppointmentViewDto> appointments
) {
}
