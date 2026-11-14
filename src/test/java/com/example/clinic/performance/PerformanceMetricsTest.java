package com.example.clinic.performance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class PerformanceMetricsTest {

    @Test
    @DisplayName("Calculates exact nearest-rank p50, p95, p99, min, mean, max, and failure rate")
    void calculatesExactPercentilesAndFailureRate() {
        List<Long> samples = IntStream.rangeClosed(1, 100)
                .mapToObj(ms -> ms * 1_000_000L)
                .toList();

        PerformanceMetrics.Summary summary = PerformanceMetrics.summarize("slot-search", samples, 0);

        assertThat(summary.totalRequests()).isEqualTo(100);
        assertThat(summary.successCount()).isEqualTo(100);
        assertThat(summary.failureCount()).isZero();
        assertThat(summary.errorRate()).isZero();
        assertThat(summary.minMs()).isCloseTo(1.0, within(0.001));
        assertThat(summary.meanMs()).isCloseTo(50.5, within(0.001));
        assertThat(summary.p50Ms()).isCloseTo(50.0, within(0.001));
        assertThat(summary.p95Ms()).isCloseTo(95.0, within(0.001));
        assertThat(summary.p99Ms()).isCloseTo(99.0, within(0.001));
        assertThat(summary.maxMs()).isCloseTo(100.0, within(0.001));
        assertThat(summary.formatLogLine()).contains("[NFR-008-PERF]").contains("p95Ms=95.00");
    }

    @Test
    @DisplayName("Tracks failures cleanly and guards invalid sample collections")
    void tracksFailuresAndRejectsInvalidInput() {
        List<Long> samples = List.of(10_000_000L, 20_000_000L, 30_000_000L, 40_000_000L);
        PerformanceMetrics.Summary summary = PerformanceMetrics.summarize("booking-tx", samples, 1);

        assertThat(summary.totalRequests()).isEqualTo(5);
        assertThat(summary.errorRate()).isCloseTo(0.20, within(0.0001));
        assertThat(summary.p95Ms()).isCloseTo(40.0, within(0.001));

        assertThatThrownBy(() -> PerformanceMetrics.summarize("empty", List.of(), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PerformanceMetrics.summarize("neg", List.of(-5L), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
