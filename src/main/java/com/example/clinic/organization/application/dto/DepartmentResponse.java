package com.example.clinic.organization.application.dto;

import java.time.Instant;
import java.util.UUID;

public record DepartmentResponse(
        UUID id,
        UUID branchId,
        String name,
        boolean active,
        Instant createdAt
) {}
