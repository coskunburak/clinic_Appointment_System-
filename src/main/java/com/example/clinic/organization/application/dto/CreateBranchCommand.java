package com.example.clinic.organization.application.dto;

public record CreateBranchCommand(
        String name,
        String timeZone,
        String address
) {
    public CreateBranchCommand {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Branch name must not be blank");
        }
        name = name.strip();
        if (timeZone != null) {
            timeZone = timeZone.strip();
        }
        if (address != null) {
            address = address.strip();
        }
    }

    public CreateBranchCommand(String name) {
        this(name, "Europe/Istanbul", null);
    }
}
