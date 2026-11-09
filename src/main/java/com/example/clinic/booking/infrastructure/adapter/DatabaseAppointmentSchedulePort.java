package com.example.clinic.booking.infrastructure.adapter;

import com.example.clinic.booking.domain.AppointmentStatus;
import com.example.clinic.booking.infrastructure.persistence.AppointmentJpaEntity;
import com.example.clinic.booking.infrastructure.persistence.AppointmentJpaRepository;
import com.example.clinic.common.domain.TimeInterval;
import com.example.clinic.scheduling.application.port.AppointmentSchedulePort;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Production database-backed adapter implementing {@link AppointmentSchedulePort}.
 * Bridges S06 appointment persistence with S05 {@link com.example.clinic.scheduling.application.AvailabilityCalculator}.
 * <p>
 * Evaluates occupied appointments across ALL branches globally for the given staff member,
 * honoring blocking statuses (PENDING, CONFIRMED, COMPLETED) and half-open {@code [start, end)} intervals.
 */
@Component
@Primary
@Transactional(readOnly = true)
public class DatabaseAppointmentSchedulePort implements AppointmentSchedulePort {

    private static final List<AppointmentStatus> BLOCKING_STATUSES = List.of(
            AppointmentStatus.PENDING,
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.COMPLETED
    );

    private final AppointmentJpaRepository appointmentJpaRepository;

    public DatabaseAppointmentSchedulePort(AppointmentJpaRepository appointmentJpaRepository) {
        this.appointmentJpaRepository = Objects.requireNonNull(appointmentJpaRepository, "appointmentJpaRepository must not be null");
    }

    @Override
    public List<TimeInterval> findOccupiedIntervals(UUID staffId, LocalDate targetDate, ZoneId branchTimeZone) {
        Objects.requireNonNull(staffId, "staffId must not be null");
        Objects.requireNonNull(targetDate, "targetDate must not be null");
        Objects.requireNonNull(branchTimeZone, "branchTimeZone must not be null");

        ZonedDateTime startOfDay = targetDate.atStartOfDay(branchTimeZone);
        ZonedDateTime endOfDay = targetDate.plusDays(1).atStartOfDay(branchTimeZone);

        Instant windowStart = startOfDay.toInstant();
        Instant windowEnd = endOfDay.toInstant();

        List<AppointmentJpaEntity> occupied = appointmentJpaRepository.findOccupiedAppointmentsForStaff(
                staffId,
                windowStart,
                windowEnd,
                BLOCKING_STATUSES
        );

        return occupied.stream()
                .map(entity -> TimeInterval.of(entity.getStartAtUtc(), entity.getEndAtUtc()))
                .toList();
    }
}
