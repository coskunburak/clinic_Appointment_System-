package com.example.clinic.booking.application;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable command payload for booking an appointment.
 *
 * @param patientId       optional patient profile ID; if provided, must match the authenticated actor
 * @param staffId         required staff profile ID
 * @param branchId        required branch ID
 * @param branchServiceId required branch service offering ID
 * @param appointmentDate required target local date
 * @param startTime       required target local start time
 * @param zoneOffset      optional explicit zone offset to resolve Daylight Saving Time (DST) overlaps
 */
public record BookingCommand(
        UUID patientId,
        UUID staffId,
        UUID branchId,
        UUID branchServiceId,
        LocalDate appointmentDate,
        LocalTime startTime,
        ZoneOffset zoneOffset
) {
    public BookingCommand {
        Objects.requireNonNull(staffId, "staffId must not be null");
        Objects.requireNonNull(branchId, "branchId must not be null");
        Objects.requireNonNull(branchServiceId, "branchServiceId must not be null");
        Objects.requireNonNull(appointmentDate, "appointmentDate must not be null");
        Objects.requireNonNull(startTime, "startTime must not be null");
    }

    public BookingCommand(UUID staffId, UUID branchId, UUID branchServiceId, LocalDate appointmentDate, LocalTime startTime) {
        this(null, staffId, branchId, branchServiceId, appointmentDate, startTime, null);
    }

    public BookingCommand(UUID patientId, UUID staffId, UUID branchId, UUID branchServiceId, LocalDate appointmentDate, LocalTime startTime) {
        this(patientId, staffId, branchId, branchServiceId, appointmentDate, startTime, null);
    }
}
