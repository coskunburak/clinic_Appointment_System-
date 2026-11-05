package com.example.clinic.organization.application.dto;

import java.util.Objects;

public record CreateOrganizationCommand(String name) {

    public CreateOrganizationCommand {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Organization name must not be blank");
        }
        name = name.strip();
    }
}
