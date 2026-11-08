package com.example.clinic.scheduling.application.port;

import com.example.clinic.common.domain.TimeInterval;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link AppointmentSchedulePort}.
 * Serves as an in-memory / placeholder provider until database persistence for Appointment is added in S06.
 */
@Component
public class DefaultAppointmentSchedulePort implements AppointmentSchedulePort {

    private final Map<UUID, List<TimeInterval>> testSchedule = new ConcurrentHashMap<>();

    @Override
    public List<TimeInterval> findOccupiedIntervals(UUID staffId, LocalDate targetDate, ZoneId branchTimeZone) {
        List<TimeInterval> intervals = testSchedule.get(staffId);
        if (intervals == null || intervals.isEmpty()) {
            return Collections.emptyList();
        }

        return intervals.stream()
                .filter(interval -> interval.startAt().atZone(branchTimeZone).toLocalDate().equals(targetDate))
                .toList();
    }

    public void registerOccupiedInterval(UUID staffId, TimeInterval interval) {
        testSchedule.computeIfAbsent(staffId, k -> new ArrayList<>()).add(interval);
    }

    public void clear(UUID staffId) {
        testSchedule.remove(staffId);
    }

    public void clearAll() {
        testSchedule.clear();
    }
}
