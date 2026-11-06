package com.example.clinic.catalog.application.dto;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

public record UpdateServicePricingCommand(
        BigDecimal newFee,
        String currency
) {
    public UpdateServicePricingCommand {
        Objects.requireNonNull(newFee, "newFee must not be null");
        if (newFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("newFee cannot be negative: " + newFee);
        }
        if (currency != null) {
            if (currency.isBlank() || currency.strip().length() != 3) {
                throw new IllegalArgumentException("currency must be a 3-character ISO code: " + currency);
            }
            currency = currency.strip().toUpperCase(Locale.ROOT);
        }
    }

    public UpdateServicePricingCommand(BigDecimal newFee) {
        this(newFee, null);
    }
}
