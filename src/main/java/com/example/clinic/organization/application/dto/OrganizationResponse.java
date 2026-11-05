package com.example.clinic.organization.application.dto;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        boolean active,
        Instant createdAt
) {}
