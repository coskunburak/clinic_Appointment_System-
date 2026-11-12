package com.example.clinic.booking.application.dto;

import java.util.UUID;

/**
 * Lightweight branch projection for patient booking selection.
 */
public record BranchSummaryDto(
        UUID id,
        String name,
        String timeZone,
        String address
) {
}
