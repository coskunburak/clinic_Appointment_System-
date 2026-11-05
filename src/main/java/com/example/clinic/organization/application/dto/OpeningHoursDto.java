package com.example.clinic.organization.application.dto;

import java.time.LocalTime;
import java.util.Objects;

public record OpeningHoursDto(
        short weekday,
        LocalTime startTime,
        LocalTime endTime
) {
    public OpeningHoursDto {
        if (weekday < 1 || weekday > 7) {
            throw new IllegalArgumentException("Weekday must be between 1 (Monday) and 7 (Sunday), got: " + weekday);
        }
        Objects.requireNonNull(startTime, "startTime must not be null");
        Objects.requireNonNull(endTime, "endTime must not be null");
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException(
                    "startTime (" + startTime + ") must strictly precede endTime (" + endTime + ")"
            );
        }
    }

    public OpeningHoursDto(int weekday, LocalTime startTime, LocalTime endTime) {
        this((short) weekday, startTime, endTime);
    }
}
