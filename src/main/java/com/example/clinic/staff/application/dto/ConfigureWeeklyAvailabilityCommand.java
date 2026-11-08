package com.example.clinic.staff.application.dto;

import java.util.List;
import java.util.UUID;

public record ConfigureWeeklyAvailabilityCommand(
        UUID assignmentId,
        List<WeeklyAvailabilityItem> items
) {}
