package com.example.clinic.catalog.application.dto;

public record UpdateServiceDurationCommand(int newDurationMinutes) {

    public UpdateServiceDurationCommand {
        if (newDurationMinutes < 1 || newDurationMinutes > 480) {
            throw new IllegalArgumentException("newDurationMinutes must be between 1 and 480, got: " + newDurationMinutes);
        }
    }
}
