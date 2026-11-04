package com.example.clinic.booking.domain;

import com.example.clinic.common.domain.Money;
import com.example.clinic.common.domain.TimeInterval;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentDomainTest {

    private static final Currency TRY = Currency.getInstance("TRY");

    private ServiceSnapshot createSnapshot(int durationMinutes) {
        return new ServiceSnapshot(UUID.randomUUID(), "Kardiyoloji Muayenesi", durationMinutes, Money.of(new BigDecimal("600.00"), TRY));
    }

    @Test
    @DisplayName("Positive: Appointment aggregate initializes with PENDING status and valid invariants")
    void shouldCreateAppointmentWithPendingStatus() {
        UUID id = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID branchServiceId = UUID.randomUUID();
        Instant start = Instant.parse("2026-11-04T10:00:00Z");
        TimeInterval interval = TimeInterval.of(start, Duration.ofMinutes(30));
        ServiceSnapshot snapshot = createSnapshot(30);
        Instant createdAt = Instant.now();

        Appointment appointment = Appointment.create(id, patientId, staffId, branchId, branchServiceId,
                interval, snapshot, createdAt);

        assertThat(appointment.id()).isEqualTo(id);
        assertThat(appointment.patientId()).isEqualTo(patientId);
        assertThat(appointment.staffId()).isEqualTo(staffId);
        assertThat(appointment.branchId()).isEqualTo(branchId);
        assertThat(appointment.branchServiceId()).isEqualTo(branchServiceId);
        assertThat(appointment.interval()).isEqualTo(interval);
        assertThat(appointment.serviceSnapshot()).isEqualTo(snapshot);
        assertThat(appointment.status()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(appointment.createdAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("Negative: Inconsistent interval duration and service snapshot duration is rejected")
    void shouldRejectMismatchedIntervalAndServiceDuration() {
        UUID patientId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID branchServiceId = UUID.randomUUID();
        Instant start = Instant.parse("2026-11-04T10:00:00Z");
        TimeInterval interval = TimeInterval.of(start, Duration.ofMinutes(45)); // 45 mins
        ServiceSnapshot snapshot = createSnapshot(30); // 30 mins

        assertThatThrownBy(() -> Appointment.create(patientId, staffId, branchId, branchServiceId, interval, snapshot))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match service snapshot duration");
    }

    @Test
    @DisplayName("Negative: Null mandatory parameters are rejected")
    void shouldRejectNullMandatoryFields() {
        UUID id = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID branchServiceId = UUID.randomUUID();
        TimeInterval interval = TimeInterval.of(Instant.parse("2026-11-04T10:00:00Z"), Duration.ofMinutes(30));
        ServiceSnapshot snapshot = createSnapshot(30);
        Instant now = Instant.now();

        assertThatThrownBy(() -> Appointment.create(null, patientId, staffId, branchId, branchServiceId, interval, snapshot, now))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> Appointment.create(id, null, staffId, branchId, branchServiceId, interval, snapshot, now))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> Appointment.create(id, patientId, null, branchId, branchServiceId, interval, snapshot, now))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> Appointment.create(id, patientId, staffId, null, branchServiceId, interval, snapshot, now))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> Appointment.create(id, patientId, staffId, branchId, null, interval, snapshot, now))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> Appointment.create(id, patientId, staffId, branchId, branchServiceId, null, snapshot, now))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> Appointment.create(id, patientId, staffId, branchId, branchServiceId, interval, null, now))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> Appointment.create(id, patientId, staffId, branchId, branchServiceId, interval, snapshot, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Positive: Appointment identity equality and hashCode is stable")
    void shouldFollowIdentityEquality() {
        UUID id = UUID.randomUUID();
        Instant start = Instant.parse("2026-11-04T10:00:00Z");
        TimeInterval interval = TimeInterval.of(start, Duration.ofMinutes(30));
        ServiceSnapshot snapshot = createSnapshot(30);

        Appointment a1 = Appointment.create(id, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), interval, snapshot, Instant.now());
        Appointment a2 = Appointment.create(id, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), interval, snapshot, Instant.now());

        assertThat(a1).isEqualTo(a2);
        assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
    }
}
