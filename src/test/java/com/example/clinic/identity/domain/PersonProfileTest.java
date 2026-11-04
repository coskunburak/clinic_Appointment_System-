package com.example.clinic.identity.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonProfileTest {

    private UserAccount createDummyUser(String email) {
        return new UserAccount(email, "hashed_pw");
    }

    @Test
    @DisplayName("Positive: PatientProfile inherits common fields and behaviors from PersonProfile")
    void shouldInheritCommonFieldsInPatientProfile() {
        UserAccount user = createDummyUser("patient@clinic.test");
        PatientProfile patient = new PatientProfile(user, "Ahmet Yılmaz", "+90 555 111 22 33");

        assertThat(patient.getId()).isNotNull();
        assertThat(patient.getUserAccount()).isEqualTo(user);
        assertThat(patient.getFullName()).isEqualTo("Ahmet Yılmaz");
        assertThat(patient.getPhone()).isEqualTo("+90 555 111 22 33");
        assertThat(patient.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Positive: StaffProfile inherits common fields and retains staff-specific properties")
    void shouldInheritCommonFieldsAndRetainStaffPropertiesInStaffProfile() {
        UserAccount user = createDummyUser("dr_ayse@clinic.test");
        StaffProfile doctor = new StaffProfile(user, "Dr. Ayşe Kaya", "+905554445566", "DOCTOR", "Kardiyoloji");

        assertThat(doctor.getId()).isNotNull();
        assertThat(doctor.getUserAccount()).isEqualTo(user);
        assertThat(doctor.getFullName()).isEqualTo("Dr. Ayşe Kaya");
        assertThat(doctor.getPhone()).isEqualTo("+905554445566");
        assertThat(doctor.getStaffType()).isEqualTo("DOCTOR");
        assertThat(doctor.getSpecialty()).isEqualTo("Kardiyoloji");
        assertThat(doctor.isActive()).isTrue();

        doctor.deactivate();
        assertThat(doctor.isActive()).isFalse();

        doctor.activate();
        assertThat(doctor.isActive()).isTrue();
    }

    @Test
    @DisplayName("Positive: Turkish Unicode characters are accepted in full names")
    void shouldAcceptTurkishUnicodeNames() {
        UserAccount user = createDummyUser("turkish@clinic.test");
        PatientProfile profile = new PatientProfile(user, "Şemsi İğdeli-Çoşkunoğlu", "+905559998877");

        assertThat(profile.getFullName()).isEqualTo("Şemsi İğdeli-Çoşkunoğlu");
    }

    @Test
    @DisplayName("Negative: Blank or null full name is rejected for both subclasses")
    void shouldRejectBlankFullName() {
        UserAccount user = createDummyUser("invalid@clinic.test");

        assertThatThrownBy(() -> new PatientProfile(user, null, "+905551112233"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Full name must not be null or blank");

        assertThatThrownBy(() -> new PatientProfile(user, "   ", "+905551112233"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Full name must not be null or blank");

        assertThatThrownBy(() -> new StaffProfile(user, "", "+905551112233", "NURSE", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Full name must not be null or blank");
    }

    @Test
    @DisplayName("Positive: Null or blank phone is permitted as optional contact data")
    void shouldAllowNullOrBlankPhone() {
        UserAccount user = createDummyUser("nophone@clinic.test");
        PatientProfile profile = new PatientProfile(user, "Mehmet Öz", null);

        assertThat(profile.getPhone()).isNull();

        profile.changePhone("   ");
        assertThat(profile.getPhone()).isNull();
    }

    @Test
    @DisplayName("Negative: Malformed phone format is rejected")
    void shouldRejectMalformedPhone() {
        UserAccount user = createDummyUser("badphone@clinic.test");

        assertThatThrownBy(() -> new PatientProfile(user, "Ali Veli", "not-a-phone-123!!!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid phone number format");
    }

    @Test
    @DisplayName("Positive: Intention-revealing methods mutate state safely with validation")
    void shouldMutateViaIntentionRevealingMethods() {
        UserAccount user = createDummyUser("patient2@clinic.test");
        PatientProfile patient = new PatientProfile(user, "Kemal Sunal", "+905551234567");

        patient.rename("Kemalettin Sunal");
        assertThat(patient.getFullName()).isEqualTo("Kemalettin Sunal");

        patient.changePhone("+905559876543");
        assertThat(patient.getPhone()).isEqualTo("+905559876543");

        patient.changeContactInformation("Kemal Sunal", "+905551112233");
        assertThat(patient.getFullName()).isEqualTo("Kemal Sunal");
        assertThat(patient.getPhone()).isEqualTo("+905551112233");
    }

    @Test
    @DisplayName("OOP/LSP: Polymorphic manipulation of PatientProfile and StaffProfile through PersonProfile abstraction")
    void shouldDemonstrateLiskovSubstitutionPrinciple() {
        UserAccount u1 = createDummyUser("p@clinic.test");
        UserAccount u2 = createDummyUser("s@clinic.test");

        PersonProfile patient = new PatientProfile(u1, "Hasta Can", "+905551111111");
        PersonProfile staff = new StaffProfile(u2, "Hekim Cem", "+905552222222", "DOCTOR", "Dahiliye");

        List<PersonProfile> profiles = List.of(patient, staff);

        for (PersonProfile profile : profiles) {
            assertThat(profile.getFullName()).isNotBlank();
            assertThat(profile.getUserAccount()).isNotNull();
            assertThat(profile.getCreatedAt()).isNotNull();

            // Demonstrate uniform behavioral method on base abstraction
            profile.rename(profile.getFullName() + " (Doğrulandı)");
            assertThat(profile.getFullName()).endsWith("(Doğrulandı)");
        }
    }
}
