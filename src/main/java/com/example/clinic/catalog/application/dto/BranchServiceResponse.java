package com.example.clinic.catalog.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BranchServiceResponse(
        UUID id,
        UUID organizationId,
        UUID branchId,
        UUID serviceDefinitionId,
        String serviceName,
        int durationMinutes,
        BigDecimal feeAmount,
        String currency,
        boolean active,
        Instant createdAt
) {}
