package com.example.clinic.staff.application.dto;

import java.time.LocalTime;

public record WeeklyAvailabilityItem(
        short weekday,
        LocalTime startTime,
        LocalTime endTime
) {}
