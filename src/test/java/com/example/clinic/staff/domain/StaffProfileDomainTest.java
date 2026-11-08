package com.example.clinic.staff.domain;

import com.example.clinic.identity.domain.StaffProfile;
import com.example.clinic.identity.domain.StaffType;
import com.example.clinic.identity.domain.UserAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StaffProfileDomainTest {

    private UserAccount createDummyUser(String email) {
        return new UserAccount(email, "hashed_password_123");
    }

    @Test
    @DisplayName("TC-007 / FR-007: Register DOCTOR, NURSE, and TECHNICIAN staff types")
    void shouldDifferentiateStaffTypes() {
        UserAccount u1 = createDummyUser("doc@clinic.test");
        UserAccount u2 = createDummyUser("nurse@clinic.test");
        UserAccount u3 = createDummyUser("tech@clinic.test");

        StaffProfile doctor = new StaffProfile(u1, "Dr. Ahmet", "+905551112233", "DOCTOR", "Kardiyoloji");
        StaffProfile nurse = new StaffProfile(u2, "Hemşire Fatma", "+905552223344", "NURSE", null);
        StaffProfile technician = new StaffProfile(u3, "Teknisyen Ali", "+905553334455", "TECHNICIAN", null);

        assertThat(doctor.getStaffType()).isEqualTo("DOCTOR");
        assertThat(doctor.isDoctor()).isTrue();
        assertThat(doctor.getStaffTypeEnum()).isEqualTo(StaffType.DOCTOR);
        assertThat(doctor.getSpecialty()).isEqualTo("Kardiyoloji");

        assertThat(nurse.getStaffType()).isEqualTo("NURSE");
        assertThat(nurse.isDoctor()).isFalse();
        assertThat(nurse.getSpecialty()).isNull();

        assertThat(technician.getStaffType()).isEqualTo("TECHNICIAN");
        assertThat(technician.isDoctor()).isFalse();
        assertThat(technician.getSpecialty()).isNull();
    }

    @Test
    @DisplayName("TC-008 / FR-008: Doctor requires specialty, blank or null specialty throws IllegalArgumentException")
    void shouldRequireSpecialtyForDoctor() {
        UserAccount user = createDummyUser("doc_blank@clinic.test");

        assertThatThrownBy(() -> new StaffProfile(user, "Dr. Mehmet", "+905551112233", "DOCTOR", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Specialty is required for staff type DOCTOR");

        assertThatThrownBy(() -> new StaffProfile(user, "Dr. Mehmet", "+905551112233", "DOCTOR", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Specialty is required for staff type DOCTOR");
    }

    @Test
    @DisplayName("Negative: Unknown or invalid staff type is rejected")
    void shouldRejectInvalidStaffType() {
        UserAccount user = createDummyUser("invalid_type@clinic.test");

        assertThatThrownBy(() -> new StaffProfile(user, "Kimliksiz", "+905551112233", "ASTRONAUT", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown staffType");

        assertThatThrownBy(() -> new StaffProfile(user, "Kimliksiz", "+905551112233", "", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("staffType must not be null or blank");
    }
}
