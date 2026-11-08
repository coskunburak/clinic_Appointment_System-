package com.example.clinic.staff.application.dto;

import java.time.Instant;
import java.util.UUID;

public record StaffAssignmentResponse(
        UUID id,
        UUID staffId,
        String staffName,
        String staffType,
        String specialty,
        UUID branchId,
        String branchName,
        UUID departmentId,
        String departmentName,
        boolean active,
        Instant createdAt
) {}
