package com.example.clinic.staff.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record StaffServiceEligibilityResponse(
        UUID id,
        UUID assignmentId,
        UUID branchServiceId,
        String serviceName,
        int durationMinutes,
        BigDecimal feeAmount,
        String currency
) {}
