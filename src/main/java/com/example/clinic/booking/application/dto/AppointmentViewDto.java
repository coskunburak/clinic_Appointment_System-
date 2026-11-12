package com.example.clinic.booking.application.dto;

import com.example.clinic.booking.domain.AppointmentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Immutable presentation projection for appointment views across Patient, Staff, and Admin portals.
 * Encapsulates formatted display strings, calculated local date/time ranges in the branch timezone,
 * and authoritative action availability flags without exposing internal persistence entities.
 */
public record AppointmentViewDto(
        UUID id,
        UUID patientId,
        String patientName,
        UUID staffId,
        String staffName,
        String staffSpecialty,
        UUID branchId,
        String branchName,
        String branchTimeZone,
        UUID branchServiceId,
        String serviceName,
        Instant startAtUtc,
        Instant endAtUtc,
        LocalDate localDate,
        LocalTime localStartTime,
        LocalTime localEndTime,
        String formattedDate,
        String formattedTimeRange,
        int durationMinutes,
        BigDecimal feeAmount,
        String currency,
        AppointmentStatus status,
        String statusBadgeClass,
        String statusDisplayName,
        boolean canCancel,
        boolean canApprove,
        boolean canReject,
        boolean canComplete,
        boolean isPast
) {
}
