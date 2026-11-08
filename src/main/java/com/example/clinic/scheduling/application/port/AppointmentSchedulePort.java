package com.example.clinic.scheduling.application.port;

import com.example.clinic.common.domain.TimeInterval;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Secondary port for retrieving occupied appointment intervals for a given staff member and date.
 * Allows decoupling AvailabilityCalculator from database persistence of Appointments (scheduled for S06).
 */
public interface AppointmentSchedulePort {

    /**
     * Returns the occupied appointment time intervals (e.g. PENDING, CONFIRMED, COMPLETED)
     * for the given staff member on the target date within the specified branch time zone.
     *
     * @param staffId        the staff member UUID
     * @param targetDate     the target local date
     * @param branchTimeZone the branch time zone
     * @return list of occupied UTC intervals
     */
    List<TimeInterval> findOccupiedIntervals(UUID staffId, LocalDate targetDate, ZoneId branchTimeZone);
}
