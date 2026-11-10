package com.example.clinic.booking.domain;

import com.example.clinic.common.domain.Money;
import com.example.clinic.common.domain.TimeInterval;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentStatusTransitionTest {

    private static final Currency TRY = Currency.getInstance("TRY");

    private final Instant appointmentStart = Instant.parse("2026-11-04T10:00:00Z");
    private final Instant appointmentEnd = Instant.parse("2026-11-04T10:30:00Z");
    private final TimeInterval interval = TimeInterval.of(appointmentStart, appointmentEnd);

    private final UUID actorStaff = UUID.randomUUID();
    private final UUID actorPatient = UUID.randomUUID();

    private ServiceSnapshot snapshot;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        snapshot = new ServiceSnapshot(UUID.randomUUID(), "Dahiliye Kontrol", 30, Money.of(new BigDecimal("450.00"), TRY));
        appointment = Appointment.create(actorPatient, actorStaff, UUID.randomUUID(), UUID.randomUUID(), interval, snapshot);
    }

    private Clock fixedClock(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    @Test
    @DisplayName("Transition: PENDING -> CONFIRMED succeeds before start time")
    void shouldApprovePendingAppointmentBeforeStart() {
        Clock clock = fixedClock(appointmentStart.minus(Duration.ofMinutes(30)));

        appointment.approve(actorStaff, clock);

        assertThat(appointment.status()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Negative: PENDING -> CONFIRMED fails if appointment has already started or passed")
    void shouldRejectApprovalAfterAppointmentStart() {
        Clock clockAtStart = fixedClock(appointmentStart);
        Clock clockAfterStart = fixedClock(appointmentStart.plus(Duration.ofMinutes(5)));

        assertThatThrownBy(() -> appointment.approve(actorStaff, clockAtStart))
                .isInstanceOf(AppointmentDomainException.class)
                .hasMessageContaining("already started or passed");

        // Status must remain PENDING (failed transition does not mutate)
        assertThat(appointment.status()).isEqualTo(AppointmentStatus.PENDING);

        assertThatThrownBy(() -> appointment.approve(actorStaff, clockAfterStart))
                .isInstanceOf(AppointmentDomainException.class);
        assertThat(appointment.status()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    @DisplayName("Transition: PENDING -> REJECTED succeeds with reason")
    void shouldRejectPendingAppointment() {
        Clock clock = fixedClock(appointmentStart.minus(Duration.ofMinutes(60)));

        appointment.reject(actorStaff, "Hekim acil ameliyatta", clock);

        assertThat(appointment.status()).isEqualTo(AppointmentStatus.REJECTED);
        assertThat(appointment.status().isTerminal()).isTrue();
    }

    @Test
    @DisplayName("Transition: PENDING -> CANCELLED succeeds before start time")
    void shouldCancelPendingAppointmentBeforeStart() {
        Clock clock = fixedClock(appointmentStart.minus(Duration.ofMinutes(15)));

        appointment.cancel(actorPatient, "Hasta plan değişikliği", clock);

        assertThat(appointment.status()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appointment.status().isTerminal()).isTrue();
    }

    @Test
    @DisplayName("Transition: CONFIRMED -> CANCELLED succeeds before start time")
    void shouldCancelConfirmedAppointmentBeforeStart() {
        Clock clockBefore = fixedClock(appointmentStart.minus(Duration.ofMinutes(45)));
        appointment.approve(actorStaff, clockBefore);
        assertThat(appointment.status()).isEqualTo(AppointmentStatus.CONFIRMED);

        appointment.cancel(actorPatient, "Rahatsızlık nedeniyle erteleme", clockBefore);

        assertThat(appointment.status()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("Negative: Cancellation after start time is rejected")
    void shouldRejectCancellationAfterStart() {
        Clock clockBefore = fixedClock(appointmentStart.minus(Duration.ofMinutes(10)));
        appointment.approve(actorStaff, clockBefore);

        Clock clockAfter = fixedClock(appointmentStart.plus(Duration.ofMinutes(1)));

        assertThatThrownBy(() -> appointment.cancel(actorPatient, "Geç kaldım", clockAfter))
                .isInstanceOf(AppointmentDomainException.class)
                .hasMessageContaining("already started");

        assertThat(appointment.status()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Transition: CONFIRMED -> COMPLETED succeeds after end time")
    void shouldCompleteConfirmedAppointmentAfterEnd() {
        Clock clockBefore = fixedClock(appointmentStart.minus(Duration.ofHours(1)));
        appointment.approve(actorStaff, clockBefore);

        Clock clockAfterEnd = fixedClock(appointmentEnd.plus(Duration.ofMinutes(5)));
        appointment.complete(actorStaff, clockAfterEnd);

        assertThat(appointment.status()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(appointment.status().isTerminal()).isTrue();
    }

    @Test
    @DisplayName("Negative: CONFIRMED -> COMPLETED fails before appointment end time")
    void shouldRejectCompletionBeforeEndTime() {
        Clock clockBefore = fixedClock(appointmentStart.minus(Duration.ofHours(1)));
        appointment.approve(actorStaff, clockBefore);

        Clock clockDuringAppointment = fixedClock(appointmentStart.plus(Duration.ofMinutes(15))); // between 10:00 and 10:30

        assertThatThrownBy(() -> appointment.complete(actorStaff, clockDuringAppointment))
                .isInstanceOf(AppointmentDomainException.class)
                .hasMessageContaining("before its scheduled end time");

        assertThat(appointment.status()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Negative: Duplicate approval on CONFIRMED appointment is rejected (CONFIRMED -> CONFIRMED)")
    void shouldRejectDuplicateApproval() {
        Clock clock = fixedClock(appointmentStart.minus(Duration.ofMinutes(30)));
        appointment.approve(actorStaff, clock);

        assertThatThrownBy(() -> appointment.approve(actorStaff, clock))
                .isInstanceOf(AppointmentDomainException.class)
                .hasMessageContaining("INVALID_STATUS_TRANSITION");
    }

    @Test
    @DisplayName("Negative: Terminal states cannot be reactivated or transitioned")
    void shouldRejectTransitionsFromTerminalStates() {
        Clock clock = fixedClock(appointmentStart.minus(Duration.ofHours(2)));

        // 1. REJECTED cannot be approved or cancelled
        appointment.reject(actorStaff, "Reddedildi", clock);
        assertThat(appointment.status()).isEqualTo(AppointmentStatus.REJECTED);

        assertThatThrownBy(() -> appointment.approve(actorStaff, clock))
                .isInstanceOf(AppointmentDomainException.class);
        assertThatThrownBy(() -> appointment.cancel(actorPatient, "İptal", clock))
                .isInstanceOf(AppointmentDomainException.class);

        // 2. CANCELLED cannot be approved
        Appointment app2 = Appointment.create(actorPatient, actorStaff, UUID.randomUUID(), UUID.randomUUID(), interval, snapshot);
        app2.cancel(actorPatient, "Vazgeçildi", clock);
        assertThatThrownBy(() -> app2.approve(actorStaff, clock))
                .isInstanceOf(AppointmentDomainException.class);

        // 3. COMPLETED cannot be cancelled
        Appointment app3 = Appointment.create(actorPatient, actorStaff, UUID.randomUUID(), UUID.randomUUID(), interval, snapshot);
        app3.approve(actorStaff, clock);
        app3.complete(actorStaff, fixedClock(appointmentEnd.plus(Duration.ofMinutes(10))));
        assertThat(app3.status()).isEqualTo(AppointmentStatus.COMPLETED);

        assertThatThrownBy(() -> app3.cancel(actorPatient, "İptal", clock))
                .isInstanceOf(AppointmentDomainException.class);
    }

    @Test
    @DisplayName("Negative: Null actor, Clock, or blank cancellation reason is rejected")
    void shouldValidateMethodParameters() {
        Clock clock = fixedClock(appointmentStart.minus(Duration.ofMinutes(30)));

        assertThatThrownBy(() -> appointment.approve(null, clock))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("actorId must not be null");

        assertThatThrownBy(() -> appointment.approve(actorStaff, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Clock must not be null");

        assertThatThrownBy(() -> appointment.cancel(actorPatient, null, clock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cancellation reason must not be null or blank");

        assertThatThrownBy(() -> appointment.cancel(actorPatient, "   ", clock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cancellation reason must not be null or blank");
    }

    @Test
    @DisplayName("Negative: Blank, null, or whitespace-only rejection reason fails")
    void rejectWithoutReasonShouldFail() {
        Clock clock = fixedClock(appointmentStart.minus(Duration.ofMinutes(30)));

        assertThatThrownBy(() -> appointment.reject(actorStaff, null, clock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rejection reason must not be null or blank");

        assertThatThrownBy(() -> appointment.reject(actorStaff, "", clock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rejection reason must not be null or blank");

        assertThatThrownBy(() -> appointment.reject(actorStaff, "   \t\n  ", clock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rejection reason must not be null or blank");

        // Status remains PENDING
        assertThat(appointment.status()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    @DisplayName("Negative: Excessively long reasons (>500 chars) fail for both rejection and cancellation")
    void excessivelyLongReasonsFail() {
        Clock clock = fixedClock(appointmentStart.minus(Duration.ofMinutes(30)));
        String longReason = "A".repeat(501);

        assertThatThrownBy(() -> appointment.reject(actorStaff, longReason, clock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not exceed 500 characters");

        assertThatThrownBy(() -> appointment.cancel(actorPatient, longReason, clock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not exceed 500 characters");

        assertThat(appointment.status()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    @DisplayName("Temporal: Cancellation immediately before start succeeds, at exact start is rejected")
    void cancellationTimingBoundaries() {
        // 1 millisecond before start succeeds
        Clock clockBefore = fixedClock(appointmentStart.minusMillis(1));
        appointment.cancel(actorPatient, "Son dakika acil durum", clockBefore);
        assertThat(appointment.status()).isEqualTo(AppointmentStatus.CANCELLED);

        // At exact start boundary fails
        Appointment app2 = Appointment.create(actorPatient, actorStaff, UUID.randomUUID(), UUID.randomUUID(), interval, snapshot);
        Clock clockAtStart = fixedClock(appointmentStart);
        assertThatThrownBy(() -> app2.cancel(actorPatient, "Tam vaktinde iptal", clockAtStart))
                .isInstanceOf(AppointmentDomainException.class)
                .hasMessageContaining("already started");
        assertThat(app2.status()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    @DisplayName("Temporal: Completion at the exact end boundary succeeds")
    void completionAtTheExactEndBoundarySucceeds() {
        Clock clockBefore = fixedClock(appointmentStart.minus(Duration.ofHours(1)));
        appointment.approve(actorStaff, clockBefore);

        // Exact end time
        Clock clockAtEnd = fixedClock(appointmentEnd);
        appointment.complete(actorStaff, clockAtEnd);

        assertThat(appointment.status()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    @DisplayName("Negative: Cannot approve already cancelled appointment")
    void cannotApproveAlreadyCancelledAppointment() {
        Clock clock = fixedClock(appointmentStart.minus(Duration.ofHours(1)));
        appointment.cancel(actorPatient, "Hasta vazgeçti", clock);
        assertThat(appointment.status()).isEqualTo(AppointmentStatus.CANCELLED);

        assertThatThrownBy(() -> appointment.approve(actorStaff, clock))
                .isInstanceOf(AppointmentDomainException.class)
                .hasMessageContaining("INVALID_STATUS_TRANSITION");

        assertThat(appointment.status()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("Positive: Service snapshot and fee remain unchanged across transitions")
    void shouldPreserveSnapshotAcrossTransitions() {
        Clock clock = fixedClock(appointmentStart.minus(Duration.ofMinutes(30)));
        appointment.approve(actorStaff, clock);

        assertThat(appointment.serviceSnapshot().fee().amount()).isEqualTo(new BigDecimal("450.00"));
        assertThat(appointment.serviceSnapshot().durationMinutes()).isEqualTo(30);
    }
}
