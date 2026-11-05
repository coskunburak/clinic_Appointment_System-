package com.example.clinic.organization.application.dto;

import java.time.Instant;
import java.util.UUID;

public record BranchResponse(
        UUID id,
        UUID organizationId,
        String name,
        String timeZone,
        String address,
        boolean active,
        Instant createdAt
) {}
