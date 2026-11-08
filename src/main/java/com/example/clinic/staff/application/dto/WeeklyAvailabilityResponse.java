package com.example.clinic.staff.application.dto;

import java.time.LocalTime;
import java.util.UUID;

public record WeeklyAvailabilityResponse(
        UUID id,
        UUID assignmentId,
        short weekday,
        LocalTime startTime,
        LocalTime endTime
) {}
