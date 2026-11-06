package com.example.clinic.identity.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleTest {

    @Test
    @DisplayName("Should expose exact required role names and authority strings")
    void shouldExposeRequiredRoleNamesAndAuthorities() {
        assertThat(Role.ROLE_PATIENT.name()).isEqualTo("ROLE_PATIENT");
        assertThat(Role.ROLE_PATIENT.authority()).isEqualTo("ROLE_PATIENT");

        assertThat(Role.ROLE_STAFF.name()).isEqualTo("ROLE_STAFF");
        assertThat(Role.ROLE_STAFF.authority()).isEqualTo("ROLE_STAFF");

        assertThat(Role.ROLE_ADMIN.name()).isEqualTo("ROLE_ADMIN");
        assertThat(Role.ROLE_ADMIN.authority()).isEqualTo("ROLE_ADMIN");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_PATIENT", "role_patient", "PATIENT", "patient", "  role_patient  "})
    @DisplayName("Should resolve ROLE_PATIENT deterministically regardless of casing or prefix")
    void shouldResolveRolePatient(String input) {
        assertThat(Role.from(input)).isEqualTo(Role.ROLE_PATIENT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_STAFF", "role_staff", "STAFF", "staff", "  role_staff  "})
    @DisplayName("Should resolve ROLE_STAFF deterministically regardless of casing or prefix")
    void shouldResolveRoleStaff(String input) {
        assertThat(Role.from(input)).isEqualTo(Role.ROLE_STAFF);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMIN", "role_admin", "ADMIN", "admin", "  role_admin  "})
    @DisplayName("Should resolve ROLE_ADMIN deterministically regardless of casing or prefix")
    void shouldResolveRoleAdmin(String input) {
        assertThat(Role.from(input)).isEqualTo(Role.ROLE_ADMIN);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ROLE_SUPER_ADMIN",
            "SUPER_ADMIN",
            "DOCTOR",
            "SECRETARY",
            "ANONYMOUS",
            "GUEST",
            "ROOT"
    })
    @DisplayName("Negative: unknown or unsupported roles must be rejected with fail-closed semantics")
    void shouldRejectUnknownRoles(String unknownRole) {
        assertThatThrownBy(() -> Role.from(unknownRole))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported or unrecognized role");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    @DisplayName("Negative: blank or empty role values must be rejected")
    void shouldRejectBlankRole(String blank) {
        assertThatThrownBy(() -> Role.from(blank))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null or blank");
    }

    @Test
    @DisplayName("Negative: null role must be rejected")
    void shouldRejectNullRole() {
        assertThatThrownBy(() -> Role.from(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null or blank");
    }
}
