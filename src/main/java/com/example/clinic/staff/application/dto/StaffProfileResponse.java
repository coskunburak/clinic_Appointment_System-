package com.example.clinic.staff.application.dto;

import java.time.Instant;
import java.util.UUID;

public record StaffProfileResponse(
        UUID id,
        UUID userId,
        String email,
        String fullName,
        String phone,
        String staffType,
        String specialty,
        boolean active,
        Instant createdAt
) {}
