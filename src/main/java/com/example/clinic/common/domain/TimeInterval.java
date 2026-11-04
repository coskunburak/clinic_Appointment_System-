package com.example.clinic.common.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable half-open time interval representing {@code [startAt, endAt)}.
 * <p>
 * The start boundary is inclusive; the end boundary is exclusive.
 * All time calculations are evaluated strictly on UTC {@link Instant}.
 */
public final class TimeInterval implements Comparable<TimeInterval> {

    private final Instant startAt;
    private final Instant endAt;

    public TimeInterval(Instant startAt, Instant endAt) {
        Objects.requireNonNull(startAt, "startAt must not be null");
        Objects.requireNonNull(endAt, "endAt must not be null");

        if (!startAt.isBefore(endAt)) {
            if (startAt.equals(endAt)) {
                throw new IllegalArgumentException(
                        "Zero-length time intervals are rejected (startAt equals endAt: " + startAt + ")"
                );
            }
            throw new IllegalArgumentException(
                    "Reversed time interval: startAt (" + startAt + ") must be strictly before endAt (" + endAt + ")"
            );
        }

        this.startAt = startAt;
        this.endAt = endAt;
    }

    public static TimeInterval of(Instant startAt, Instant endAt) {
        return new TimeInterval(startAt, endAt);
    }

    public static TimeInterval of(Instant startAt, Duration duration) {
        Objects.requireNonNull(startAt, "startAt must not be null");
        Objects.requireNonNull(duration, "duration must not be null");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("Interval duration must be strictly positive: " + duration);
        }
        return new TimeInterval(startAt, startAt.plus(duration));
    }

    public Instant startAt() {
        return startAt;
    }

    public Instant endAt() {
        return endAt;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public Duration duration() {
        return Duration.between(startAt, endAt);
    }

    /**
     * Determines whether this half-open interval {@code [startAt, endAt)} overlaps with another interval.
     * Adjacent intervals (e.g. {@code [09:00, 09:30)} and {@code [09:30, 10:00)}) do NOT overlap.
     *
     * @param other the other interval to compare against
     * @return true if intervals overlap, false otherwise
     */
    public boolean overlapsWith(TimeInterval other) {
        Objects.requireNonNull(other, "TimeInterval operand must not be null");
        return this.startAt.isBefore(other.endAt) && other.startAt.isBefore(this.endAt);
    }

    /**
     * Checks if the given instant falls within {@code [startAt, endAt)}.
     *
     * @param instant the instant to test
     * @return true if startAt &le; instant &lt; endAt
     */
    public boolean contains(Instant instant) {
        Objects.requireNonNull(instant, "Instant must not be null");
        return !instant.isBefore(startAt) && instant.isBefore(endAt);
    }

    /**
     * Checks if the specified interval is entirely contained within this interval.
     *
     * @param other the candidate inner interval
     * @return true if this interval covers the other interval completely
     */
    public boolean contains(TimeInterval other) {
        Objects.requireNonNull(other, "TimeInterval operand must not be null");
        return !other.startAt.isBefore(this.startAt) && !other.endAt.isAfter(this.endAt);
    }

    @Override
    public int compareTo(TimeInterval other) {
        Objects.requireNonNull(other, "TimeInterval operand must not be null");
        int startComparison = this.startAt.compareTo(other.startAt);
        if (startComparison != 0) {
            return startComparison;
        }
        return this.endAt.compareTo(other.endAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeInterval that)) return false;
        return startAt.equals(that.startAt) && endAt.equals(that.endAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startAt, endAt);
    }

    @Override
    public String toString() {
        return "[" + startAt + ", " + endAt + ")";
    }
}
