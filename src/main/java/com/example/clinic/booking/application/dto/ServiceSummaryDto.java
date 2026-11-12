package com.example.clinic.booking.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service offering projection for patient booking selection.
 */
public record ServiceSummaryDto(
        UUID id,
        String name,
        String description,
        int durationMinutes,
        BigDecimal feeAmount,
        String currency
) {
}
