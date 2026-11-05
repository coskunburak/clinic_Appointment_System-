package com.example.clinic.catalog.application.dto;

public record CreateServiceCommand(
        String name,
        String description
) {
    public CreateServiceCommand {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Service name must not be blank");
        }
        name = name.strip();
        if (description != null) {
            description = description.strip();
        }
    }

    public CreateServiceCommand(String name) {
        this(name, null);
    }
}
