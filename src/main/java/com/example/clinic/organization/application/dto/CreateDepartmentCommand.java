package com.example.clinic.organization.application.dto;

public record CreateDepartmentCommand(String name) {

    public CreateDepartmentCommand {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Department name must not be blank");
        }
        name = name.strip();
    }
}
