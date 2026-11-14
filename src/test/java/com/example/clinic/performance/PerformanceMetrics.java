package com.example.clinic.performance;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Deterministic latency percentile and error-rate calculator using monotonic nanosecond timings (NFR-008, TC-044).
 */
public final class PerformanceMetrics {

    private PerformanceMetrics() {
    }

    public static Summary summarize(String operationName, List<Long> durationsNanos, int failureCount) {
        Objects.requireNonNull(operationName, "operationName must not be null");
        Objects.requireNonNull(durationsNanos, "durationsNanos must not be null");
        if (failureCount < 0) {
            throw new IllegalArgumentException("failureCount must not be negative");
        }
        int successCount = durationsNanos.size();
        int totalRequests = successCount + failureCount;
        if (totalRequests == 0) {
            throw new IllegalArgumentException("At least one sample or failure must be recorded");
        }
        if (successCount == 0) {
            return new Summary(operationName, totalRequests, 0, failureCount, 1.0,
                    Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        }

        long[] sorted = durationsNanos.stream().mapToLong(Long::longValue).toArray();
        Arrays.sort(sorted);

        double sumMs = 0.0;
        for (long nanos : sorted) {
            if (nanos < 0) {
                throw new IllegalArgumentException("Duration must not be negative: " + nanos);
            }
            sumMs += nanos / 1_000_000.0;
        }

        double minMs = sorted[0] / 1_000_000.0;
        double meanMs = sumMs / sorted.length;
        double p50Ms = percentileMillis(sorted, 0.50);
        double p95Ms = percentileMillis(sorted, 0.95);
        double p99Ms = percentileMillis(sorted, 0.99);
        double maxMs = sorted[sorted.length - 1] / 1_000_000.0;
        double errorRate = (double) failureCount / totalRequests;

        return new Summary(operationName, totalRequests, successCount, failureCount, errorRate,
                minMs, meanMs, p50Ms, p95Ms, p99Ms, maxMs);
    }

    /**
     * Nearest-rank percentile calculation on sorted nanosecond samples.
     */
    public static double percentileMillis(long[] sortedNanos, double percentile) {
        if (sortedNanos == null || sortedNanos.length == 0) {
            throw new IllegalArgumentException("sortedNanos must not be empty");
        }
        if (percentile <= 0.0 || percentile > 1.0) {
            throw new IllegalArgumentException("percentile must be in (0.0, 1.0]");
        }
        int index = (int) Math.ceil(percentile * sortedNanos.length) - 1;
        int clamped = Math.max(0, Math.min(index, sortedNanos.length - 1));
        return sortedNanos[clamped] / 1_000_000.0;
    }

    public record Summary(
            String operationName,
            int totalRequests,
            int successCount,
            int failureCount,
            double errorRate,
            double minMs,
            double meanMs,
            double p50Ms,
            double p95Ms,
            double p99Ms,
            double maxMs
    ) {
        public String formatLogLine() {
            return String.format(
                    Locale.ROOT,
                    "[NFR-008-PERF] operation=%s total=%d success=%d failures=%d errorRate=%.4f minMs=%.2f meanMs=%.2f p50Ms=%.2f p95Ms=%.2f p99Ms=%.2f maxMs=%.2f",
                    operationName, totalRequests, successCount, failureCount, errorRate,
                    minMs, meanMs, p50Ms, p95Ms, p99Ms, maxMs
            );
        }
    }
}
