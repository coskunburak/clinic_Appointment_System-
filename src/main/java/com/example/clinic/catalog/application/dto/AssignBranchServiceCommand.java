package com.example.clinic.catalog.application.dto;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record AssignBranchServiceCommand(
        UUID serviceDefinitionId,
        int durationMinutes,
        BigDecimal fee,
        String currency
) {
    public AssignBranchServiceCommand {
        Objects.requireNonNull(serviceDefinitionId, "serviceDefinitionId must not be null");
        if (durationMinutes < 1 || durationMinutes > 480) {
            throw new IllegalArgumentException("durationMinutes must be between 1 and 480, got: " + durationMinutes);
        }
        Objects.requireNonNull(fee, "fee must not be null");
        if (fee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("fee cannot be negative: " + fee);
        }
        if (currency == null || currency.isBlank() || currency.strip().length() != 3) {
            throw new IllegalArgumentException("currency must be a 3-character ISO code: " + currency);
        }
        currency = currency.strip().toUpperCase();
    }

    public AssignBranchServiceCommand(UUID serviceDefinitionId, int durationMinutes, BigDecimal fee) {
        this(serviceDefinitionId, durationMinutes, fee, "TRY");
    }
}
