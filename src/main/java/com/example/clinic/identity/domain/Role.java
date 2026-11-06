package com.example.clinic.identity.domain;

import java.util.Locale;
import java.util.Objects;

/**
 * Type-safe enumeration of the three fundamental system roles required by the academic specification.
 * Satisfies WP-S04-01, FR-022, FR-023, and FR-024.
 * <p>
 * Supported roles:
 * <ul>
 *   <li>{@link #ROLE_PATIENT}: Individual managing their own profile and booking requests.</li>
 *   <li>{@link #ROLE_STAFF}: Medical or clinic staff operating within their assigned branch scope.</li>
 *   <li>{@link #ROLE_ADMIN}: Hospital or clinic administrator managing authorized organization/branch resources.</li>
 * </ul>
 * Rejects unknown, malformed, null, or unsupported role names with fail-closed semantics.
 */
public enum Role {

    ROLE_PATIENT,
    ROLE_STAFF,
    ROLE_ADMIN;

    public String authority() {
        return name();
    }

    /**
     * Resolves a role from a string representation in a deterministic, fail-closed manner.
     * Rejects unknown, empty, or unsupported roles without granting privileges.
     *
     * @param value raw role string (e.g. "ROLE_ADMIN", "ADMIN")
     * @return matching canonical {@link Role}
     * @throws IllegalArgumentException if the role is null, blank, or unsupported
     */
    public static Role from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Role representation must not be null or blank");
        }
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }

        for (Role candidate : values()) {
            if (candidate.name().equals(normalized)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unsupported or unrecognized role: " + value);
    }
}
