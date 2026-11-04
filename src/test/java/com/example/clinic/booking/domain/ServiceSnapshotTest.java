package com.example.clinic.booking.domain;

import com.example.clinic.common.domain.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceSnapshotTest {

    private static final Currency TRY = Currency.getInstance("TRY");

    @Test
    @DisplayName("Positive: Valid snapshot is created and values are exposed correctly")
    void shouldCreateValidServiceSnapshot() {
        UUID serviceId = UUID.randomUUID();
        Money fee = Money.of(new BigDecimal("750.00"), TRY);

        ServiceSnapshot snapshot = new ServiceSnapshot(serviceId, "Genel Muayene", 30, fee);

        assertThat(snapshot.serviceId()).isEqualTo(serviceId);
        assertThat(snapshot.serviceName()).isEqualTo("Genel Muayene");
        assertThat(snapshot.durationMinutes()).isEqualTo(30);
        assertThat(snapshot.fee()).isEqualTo(fee);
    }

    @Test
    @DisplayName("Negative: Blank or null service name is rejected")
    void shouldRejectBlankServiceName() {
        UUID serviceId = UUID.randomUUID();
        Money fee = Money.of(new BigDecimal("500.00"), TRY);

        assertThatThrownBy(() -> new ServiceSnapshot(serviceId, null, 30, fee))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("serviceName must not be blank");

        assertThatThrownBy(() -> new ServiceSnapshot(serviceId, "   ", 30, fee))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("serviceName must not be blank");
    }

    @Test
    @DisplayName("Negative: Zero or negative duration is rejected")
    void shouldRejectNonPositiveDuration() {
        UUID serviceId = UUID.randomUUID();
        Money fee = Money.of(new BigDecimal("500.00"), TRY);

        assertThatThrownBy(() -> new ServiceSnapshot(serviceId, "Diş Temizliği", 0, fee))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("durationMinutes must be between 1 and 480");

        assertThatThrownBy(() -> new ServiceSnapshot(serviceId, "Diş Temizliği", -15, fee))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("durationMinutes must be between 1 and 480");
    }

    @Test
    @DisplayName("Negative: Duration exceeding 480 minutes (8 hours) is rejected")
    void shouldRejectDurationExceedingMaximum() {
        UUID serviceId = UUID.randomUUID();
        Money fee = Money.of(new BigDecimal("500.00"), TRY);

        assertThatThrownBy(() -> new ServiceSnapshot(serviceId, "Kapsamlı Ameliyat", 481, fee))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("durationMinutes must be between 1 and 480");
    }

    @Test
    @DisplayName("Negative: Null fee or null serviceId is rejected")
    void shouldRejectNullFeeOrId() {
        UUID serviceId = UUID.randomUUID();

        assertThatThrownBy(() -> new ServiceSnapshot(null, "Ekokardiyografi", 45, Money.of("1000.00", TRY)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("serviceId must not be null");

        assertThatThrownBy(() -> new ServiceSnapshot(serviceId, "Ekokardiyografi", 45, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("fee must not be null");
    }

    @Test
    @DisplayName("Positive: Value equality and hashCode consistency for matching snapshots")
    void shouldDemonstrateValueEquality() {
        UUID serviceId = UUID.randomUUID();
        Money fee = Money.of(new BigDecimal("400.00"), TRY);

        ServiceSnapshot snap1 = new ServiceSnapshot(serviceId, "Check-up", 60, fee);
        ServiceSnapshot snap2 = new ServiceSnapshot(serviceId, "Check-up", 60, fee);

        assertThat(snap1).isEqualTo(snap2);
        assertThat(snap1.hashCode()).isEqualTo(snap2.hashCode());
    }

    @Test
    @DisplayName("Positive: Snapshot retains its values independently of subsequent external mutations")
    void shouldRemainImmutableAndIsolated() {
        UUID serviceId = UUID.randomUUID();
        Money originalFee = Money.of(new BigDecimal("300.00"), TRY);

        ServiceSnapshot snapshot = new ServiceSnapshot(serviceId, "Konsültasyon", 20, originalFee);

        // Even if a new fee is calculated externally, snapshot remains intact
        Money updatedFee = originalFee.add(Money.of("50.00", TRY));

        assertThat(snapshot.fee().amount()).isEqualTo(new BigDecimal("300.00"));
        assertThat(updatedFee.amount()).isEqualTo(new BigDecimal("350.00"));
    }
}
