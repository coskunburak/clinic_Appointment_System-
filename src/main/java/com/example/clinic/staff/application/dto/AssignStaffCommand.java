package com.example.clinic.staff.application.dto;

import java.util.UUID;

public record AssignStaffCommand(
        UUID staffId,
        UUID branchId,
        UUID departmentId
) {}
