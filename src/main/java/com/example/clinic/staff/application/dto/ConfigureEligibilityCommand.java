package com.example.clinic.staff.application.dto;

import java.util.List;
import java.util.UUID;

public record ConfigureEligibilityCommand(
        UUID assignmentId,
        List<UUID> branchServiceIds
) {}
