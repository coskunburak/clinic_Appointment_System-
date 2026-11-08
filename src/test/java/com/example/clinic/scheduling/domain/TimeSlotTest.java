package com.example.clinic.scheduling.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeSlotTest {

    @Test
    @DisplayName("TimeSlot: Validates invariants, formatting, and ordering")
    void shouldValidateTimeSlotInvariantsAndOrdering() {
        TimeSlot slot1 = new TimeSlot(LocalTime.of(9, 0), LocalTime.of(9, 30));
        TimeSlot slot2 = new TimeSlot(LocalTime.of(9, 30), LocalTime.of(10, 0));

        assertThat(slot1.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(slot1.endTime()).isEqualTo(LocalTime.of(9, 30));
        assertThat(slot1.toString()).isEqualTo("09:00-09:30");
        assertThat(slot1.compareTo(slot2)).isNegative();

        assertThatThrownBy(() -> new TimeSlot(LocalTime.of(10, 0), LocalTime.of(9, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startTime");
    }

    @Test
    @DisplayName("TimeSlot: Overlaps detection works accurately")
    void shouldDetectOverlaps() {
        TimeSlot slot = new TimeSlot(LocalTime.of(10, 0), LocalTime.of(10, 30));

        assertThat(slot.overlaps(LocalTime.of(9, 30), LocalTime.of(10, 15))).isTrue();
        assertThat(slot.overlaps(LocalTime.of(10, 0), LocalTime.of(10, 30))).isTrue();
        assertThat(slot.overlaps(LocalTime.of(10, 15), LocalTime.of(10, 45))).isTrue();

        // Boundary abutment does not overlap [start, end)
        assertThat(slot.overlaps(LocalTime.of(9, 30), LocalTime.of(10, 0))).isFalse();
        assertThat(slot.overlaps(LocalTime.of(10, 30), LocalTime.of(11, 0))).isFalse();
    }
}
