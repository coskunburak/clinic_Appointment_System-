package com.example.clinic.catalog.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ServiceDefinitionResponse(
        UUID id,
        UUID organizationId,
        String name,
        String description,
        boolean active,
        Instant createdAt
) {}
