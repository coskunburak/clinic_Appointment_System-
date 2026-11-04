package com.example.clinic.booking.domain;

import com.example.clinic.common.domain.Money;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable snapshot of the medical service details captured at the exact moment of appointment booking.
 * <p>
 * Ensures historical audit fidelity: subsequent price or duration updates in the service catalog
 * do not alter past booked appointments (Snapshot Pattern).
 */
public final class ServiceSnapshot {

    public static final int MIN_DURATION_MINUTES = 1;
    public static final int MAX_DURATION_MINUTES = 480;

    private final UUID serviceId;
    private final String serviceName;
    private final int durationMinutes;
    private final Money fee;

    public ServiceSnapshot(UUID serviceId, String serviceName, int durationMinutes, Money fee) {
        this.serviceId = Objects.requireNonNull(serviceId, "serviceId must not be null");

        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("serviceName must not be blank");
        }
        this.serviceName = serviceName.strip();

        if (durationMinutes < MIN_DURATION_MINUTES || durationMinutes > MAX_DURATION_MINUTES) {
            throw new IllegalArgumentException(
                    "durationMinutes must be between " + MIN_DURATION_MINUTES + " and " + MAX_DURATION_MINUTES +
                            ", got: " + durationMinutes
            );
        }
        this.durationMinutes = durationMinutes;

        this.fee = Objects.requireNonNull(fee, "fee must not be null");
    }

    public UUID serviceId() {
        return serviceId;
    }

    public String serviceName() {
        return serviceName;
    }

    public int durationMinutes() {
        return durationMinutes;
    }

    public Money fee() {
        return fee;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public Money getFee() {
        return fee;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceSnapshot that)) return false;
        return durationMinutes == that.durationMinutes &&
                serviceId.equals(that.serviceId) &&
                serviceName.equals(that.serviceName) &&
                fee.equals(that.fee);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId, serviceName, durationMinutes, fee);
    }

    @Override
    public String toString() {
        return "ServiceSnapshot{" +
                "serviceId=" + serviceId +
                ", serviceName='" + serviceName + '\'' +
                ", durationMinutes=" + durationMinutes +
                ", fee=" + fee +
                '}';
    }
}
