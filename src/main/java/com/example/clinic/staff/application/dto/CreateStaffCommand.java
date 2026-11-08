package com.example.clinic.staff.application.dto;

import java.util.UUID;

public record CreateStaffCommand(
        UUID organizationId,
        UUID branchId,
        String email,
        String password,
        String fullName,
        String phone,
        String staffType,
        String specialty
) {}
