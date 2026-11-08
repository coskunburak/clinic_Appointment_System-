package com.example.clinic.scheduling.domain;

import java.time.LocalTime;
import java.util.Objects;

/**
 * Pure domain value object representing a discrete time slot candidate.
 * Fulfills WP-S05-02 and SCH-01..SCH-05.
 * Strictly free of Spring, JPA, Hibernate, and JDBC dependencies.
 */
public record TimeSlot(LocalTime startTime, LocalTime endTime) implements Comparable<TimeSlot> {

    public TimeSlot {
        Objects.requireNonNull(startTime, "startTime must not be null");
        Objects.requireNonNull(endTime, "endTime must not be null");
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException(
                    "startTime (" + startTime + ") must be before endTime (" + endTime + ")"
            );
        }
    }

    public boolean overlaps(LocalTime otherStart, LocalTime otherEnd) {
        Objects.requireNonNull(otherStart, "otherStart must not be null");
        Objects.requireNonNull(otherEnd, "otherEnd must not be null");
        return this.startTime.isBefore(otherEnd) && otherStart.isBefore(this.endTime);
    }

    public boolean fitsInside(LocalTime windowStart, LocalTime windowEnd) {
        Objects.requireNonNull(windowStart, "windowStart must not be null");
        Objects.requireNonNull(windowEnd, "windowEnd must not be null");
        return !this.startTime.isBefore(windowStart) && !this.endTime.isAfter(windowEnd);
    }

    @Override
    public int compareTo(TimeSlot other) {
        int cmp = this.startTime.compareTo(other.startTime);
        if (cmp != 0) {
            return cmp;
        }
        return this.endTime.compareTo(other.endTime);
    }

    @Override
    public String toString() {
        return startTime + "-" + endTime;
    }
}
