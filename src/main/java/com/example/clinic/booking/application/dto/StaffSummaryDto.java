package com.example.clinic.booking.application.dto;

import java.util.UUID;

/**
 * Eligible clinician projection for patient booking selection.
 */
public record StaffSummaryDto(
        UUID id,
        String fullName,
        String specialty,
        String staffType
) {
}
