package com.example.clinic.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeIntervalTest {

    private final Instant t0900 = Instant.parse("2026-11-04T09:00:00Z");
    private final Instant t0915 = Instant.parse("2026-11-04T09:15:00Z");
    private final Instant t0930 = Instant.parse("2026-11-04T09:30:00Z");
    private final Instant t0945 = Instant.parse("2026-11-04T09:45:00Z");
    private final Instant t1000 = Instant.parse("2026-11-04T10:00:00Z");

    @Test
    @DisplayName("Boundary: Exact adjacent intervals do NOT overlap [09:00, 09:30) and [09:30, 10:00)")
    void shouldNotOverlapForExactAdjacentIntervals() {
        TimeInterval first = TimeInterval.of(t0900, t0930);
        TimeInterval second = TimeInterval.of(t0930, t1000);

        assertThat(first.overlapsWith(second)).isFalse();
        assertThat(second.overlapsWith(first)).isFalse(); // Symmetry
    }

    @Test
    @DisplayName("Positive: Partial intersection overlaps [09:00, 09:30) and [09:15, 09:45)")
    void shouldOverlapForPartialIntersection() {
        TimeInterval first = TimeInterval.of(t0900, t0930);
        TimeInterval second = TimeInterval.of(t0915, t0945);

        assertThat(first.overlapsWith(second)).isTrue();
        assertThat(second.overlapsWith(first)).isTrue(); // Symmetry
    }

    @Test
    @DisplayName("Positive: Completely contained interval overlaps [09:00, 10:00) and [09:15, 09:30)")
    void shouldOverlapForCompletelyContainedInterval() {
        TimeInterval outer = TimeInterval.of(t0900, t1000);
        TimeInterval inner = TimeInterval.of(t0915, t0930);

        assertThat(outer.overlapsWith(inner)).isTrue();
        assertThat(inner.overlapsWith(outer)).isTrue();
        assertThat(outer.contains(inner)).isTrue();
        assertThat(inner.contains(outer)).isFalse();
    }

    @Test
    @DisplayName("Positive: Exactly identical intervals overlap")
    void shouldOverlapForIdenticalIntervals() {
        TimeInterval a = TimeInterval.of(t0900, t0930);
        TimeInterval b = TimeInterval.of(t0900, t0930);

        assertThat(a.overlapsWith(b)).isTrue();
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("Positive: Same start, different end intervals overlap")
    void shouldOverlapWhenSameStartDifferentEnd() {
        TimeInterval shorter = TimeInterval.of(t0900, t0930);
        TimeInterval longer = TimeInterval.of(t0900, t1000);

        assertThat(shorter.overlapsWith(longer)).isTrue();
        assertThat(longer.overlapsWith(shorter)).isTrue();
    }

    @Test
    @DisplayName("Positive: Same end, different start intervals overlap")
    void shouldOverlapWhenSameEndDifferentStart() {
        TimeInterval earlier = TimeInterval.of(t0900, t1000);
        TimeInterval later = TimeInterval.of(t0930, t1000);

        assertThat(earlier.overlapsWith(later)).isTrue();
        assertThat(later.overlapsWith(earlier)).isTrue();
    }

    @Test
    @DisplayName("Boundary: 1-nanosecond intersection overlaps")
    void shouldOverlapForOneNanosecondIntersection() {
        Instant boundary = Instant.parse("2026-11-04T12:00:00.000000000Z");
        Instant oneNanoAfter = boundary.plusNanos(1);
        Instant endLater = boundary.plusSeconds(60);

        TimeInterval first = TimeInterval.of(boundary.minusSeconds(60), oneNanoAfter);
        TimeInterval second = TimeInterval.of(boundary, endLater);

        assertThat(first.overlapsWith(second)).isTrue();
        assertThat(second.overlapsWith(first)).isTrue();
    }

    @Test
    @DisplayName("Boundary: 1-nanosecond separation does NOT overlap")
    void shouldNotOverlapForOneNanosecondSeparation() {
        Instant boundary = Instant.parse("2026-11-04T12:00:00.000000000Z");
        Instant oneNanoAfter = boundary.plusNanos(1);

        TimeInterval first = TimeInterval.of(boundary.minusSeconds(60), boundary);
        TimeInterval second = TimeInterval.of(oneNanoAfter, boundary.plusSeconds(60));

        assertThat(first.overlapsWith(second)).isFalse();
        assertThat(second.overlapsWith(first)).isFalse();
    }

    @Test
    @DisplayName("Boundary: Start boundary is included, end boundary is excluded")
    void shouldRespectHalfOpenBoundaries() {
        TimeInterval interval = TimeInterval.of(t0900, t0930);

        assertThat(interval.contains(t0900)).isTrue(); // start inclusive
        assertThat(interval.contains(t0915)).isTrue(); // intermediate
        assertThat(interval.contains(t0930)).isFalse(); // end exclusive
        assertThat(interval.contains(t0900.minusNanos(1))).isFalse();
        assertThat(interval.contains(t0930.plusNanos(1))).isFalse();
    }

    @Test
    @DisplayName("Negative: Zero-length interval is rejected")
    void shouldRejectZeroLengthInterval() {
        assertThatThrownBy(() -> TimeInterval.of(t0900, t0900))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Zero-length time intervals are rejected");
    }

    @Test
    @DisplayName("Negative: Reversed interval (start after end) is rejected")
    void shouldRejectReversedInterval() {
        assertThatThrownBy(() -> TimeInterval.of(t1000, t0900))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reversed time interval");
    }

    @Test
    @DisplayName("Negative: Null start or end instant is rejected")
    void shouldRejectNullInstants() {
        assertThatThrownBy(() -> TimeInterval.of(null, t1000))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("startAt must not be null");

        assertThatThrownBy(() -> TimeInterval.of(t0900, (Instant) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("endAt must not be null");

        assertThatThrownBy(() -> TimeInterval.of(t0900, (Duration) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("duration must not be null");
    }

    @Test
    @DisplayName("Positive: Duration calculation matches actual temporal length")
    void shouldCalculateCorrectDuration() {
        TimeInterval interval = TimeInterval.of(t0900, t0930);
        assertThat(interval.duration()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("Positive: Factory with Duration creates expected half-open interval")
    void shouldConstructWithDurationFactory() {
        TimeInterval interval = TimeInterval.of(t0900, Duration.ofMinutes(45));
        assertThat(interval.startAt()).isEqualTo(t0900);
        assertThat(interval.endAt()).isEqualTo(t0945);
        assertThat(interval.duration()).isEqualTo(Duration.ofMinutes(45));
    }

    @Test
    @DisplayName("Negative: Non-positive duration factory call is rejected")
    void shouldRejectNonPositiveDuration() {
        assertThatThrownBy(() -> TimeInterval.of(t0900, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> TimeInterval.of(t0900, Duration.ofMinutes(-10)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
