package com.example.clinic.booking.infrastructure.persistence;

import com.example.clinic.booking.domain.Appointment;
import com.example.clinic.booking.domain.AppointmentStatus;
import com.example.clinic.booking.domain.ServiceSnapshot;
import com.example.clinic.common.domain.BaseEntity;
import com.example.clinic.common.domain.Money;
import com.example.clinic.common.domain.TimeInterval;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

/**
 * Dedicated JPA infrastructure entity mapping relational persistence to the pure domain aggregate {@link Appointment}.
 * <p>
 * Ensures Spring/JPA independence for the pure domain core while supporting optimistic concurrency control via {@code @Version}.
 */
@Entity
@Table(name = "appointment")
public class AppointmentJpaEntity extends BaseEntity {

    @NotNull
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @NotNull
    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @NotNull
    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @NotNull
    @Column(name = "branch_service_id", nullable = false)
    private UUID branchServiceId;

    @NotNull
    @Column(name = "start_at_utc", nullable = false)
    private Instant startAtUtc;

    @NotNull
    @Column(name = "end_at_utc", nullable = false)
    private Instant endAtUtc;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AppointmentStatus status;

    @NotNull
    @Column(name = "service_name_snapshot", nullable = false)
    private String serviceNameSnapshot;

    @Column(name = "duration_minutes_snapshot", nullable = false)
    private int durationMinutesSnapshot;

    @NotNull
    @Column(name = "fee_amount_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal feeAmountSnapshot;

    @NotNull
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency_snapshot", nullable = false, length = 3)
    private String currencySnapshot;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    protected AppointmentJpaEntity() {
        super();
    }

    public AppointmentJpaEntity(UUID id, UUID patientId, UUID staffId, UUID branchId, UUID branchServiceId,
                                Instant startAtUtc, Instant endAtUtc, AppointmentStatus status,
                                String serviceNameSnapshot, int durationMinutesSnapshot,
                                BigDecimal feeAmountSnapshot, String currencySnapshot,
                                Long version, Instant createdAt) {
        super(id, createdAt);
        this.patientId = Objects.requireNonNull(patientId, "patientId must not be null");
        this.staffId = Objects.requireNonNull(staffId, "staffId must not be null");
        this.branchId = Objects.requireNonNull(branchId, "branchId must not be null");
        this.branchServiceId = Objects.requireNonNull(branchServiceId, "branchServiceId must not be null");
        this.startAtUtc = Objects.requireNonNull(startAtUtc, "startAtUtc must not be null");
        this.endAtUtc = Objects.requireNonNull(endAtUtc, "endAtUtc must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.serviceNameSnapshot = Objects.requireNonNull(serviceNameSnapshot, "serviceNameSnapshot must not be null");
        this.durationMinutesSnapshot = durationMinutesSnapshot;
        this.feeAmountSnapshot = Objects.requireNonNull(feeAmountSnapshot, "feeAmountSnapshot must not be null");
        this.currencySnapshot = Objects.requireNonNull(currencySnapshot, "currencySnapshot must not be null");
        this.version = version != null ? version : 0L;
    }

    public static AppointmentJpaEntity fromDomain(Appointment appointment, Long version) {
        Objects.requireNonNull(appointment, "appointment must not be null");
        ServiceSnapshot snapshot = appointment.getServiceSnapshot();
        return new AppointmentJpaEntity(
                appointment.getId(),
                appointment.getPatientId(),
                appointment.getStaffId(),
                appointment.getBranchId(),
                appointment.getBranchServiceId(),
                appointment.getInterval().startAt(),
                appointment.getInterval().endAt(),
                appointment.getStatus(),
                snapshot.serviceName(),
                snapshot.durationMinutes(),
                snapshot.fee().amount(),
                snapshot.fee().currency().getCurrencyCode(),
                version,
                appointment.getCreatedAt()
        );
    }

    public Appointment toDomain() {
        TimeInterval interval = TimeInterval.of(startAtUtc, endAtUtc);
        ServiceSnapshot snapshot = new ServiceSnapshot(
                branchServiceId,
                serviceNameSnapshot,
                durationMinutesSnapshot,
                Money.of(feeAmountSnapshot, Currency.getInstance(currencySnapshot))
        );
        return Appointment.reconstitute(
                getId(),
                patientId,
                staffId,
                branchId,
                branchServiceId,
                interval,
                snapshot,
                status,
                getCreatedAt(),
                version
        );
    }

    public UUID getPatientId() {
        return patientId;
    }

    public UUID getStaffId() {
        return staffId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public UUID getBranchServiceId() {
        return branchServiceId;
    }

    public Instant getStartAtUtc() {
        return startAtUtc;
    }

    public Instant getEndAtUtc() {
        return endAtUtc;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public String getServiceNameSnapshot() {
        return serviceNameSnapshot;
    }

    public int getDurationMinutesSnapshot() {
        return durationMinutesSnapshot;
    }

    public BigDecimal getFeeAmountSnapshot() {
        return feeAmountSnapshot;
    }

    public String getCurrencySnapshot() {
        return currencySnapshot;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
