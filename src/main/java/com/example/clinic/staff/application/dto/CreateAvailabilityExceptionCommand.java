package com.example.clinic.staff.application.dto;

import com.example.clinic.staff.domain.AvailabilityExceptionKind;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateAvailabilityExceptionCommand(
        UUID assignmentId,
        LocalDate localDate,
        AvailabilityExceptionKind kind,
        LocalTime startTime,
        LocalTime endTime,
        String reason
) {}
