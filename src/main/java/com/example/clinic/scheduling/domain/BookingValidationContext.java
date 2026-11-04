package com.example.clinic.scheduling.domain;

import com.example.clinic.booking.domain.AppointmentStatus;
import com.example.clinic.common.domain.TimeInterval;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable domain context carrying verified scheduling facts required for polymorphic constraint validation.
 * <p>
 * Contains no Spring or database dependencies; all collections are defensively copied.
 */
public final class BookingValidationContext {

    public record BranchOpeningWindow(DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime) {
        public BranchOpeningWindow {
            Objects.requireNonNull(dayOfWeek, "dayOfWeek must not be null");
            Objects.requireNonNull(openTime, "openTime must not be null");
            Objects.requireNonNull(closeTime, "closeTime must not be null");
            if (!openTime.isBefore(closeTime)) {
                throw new IllegalArgumentException(
                        "Branch openTime (" + openTime + ") must be before closeTime (" + closeTime + ")"
                );
            }
        }
    }

    public record StaffWorkingWindow(UUID staffId, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        public StaffWorkingWindow {
            Objects.requireNonNull(staffId, "staffId must not be null");
            Objects.requireNonNull(dayOfWeek, "dayOfWeek must not be null");
            Objects.requireNonNull(startTime, "startTime must not be null");
            Objects.requireNonNull(endTime, "endTime must not be null");
            if (!startTime.isBefore(endTime)) {
                throw new IllegalArgumentException(
                        "Staff startTime (" + startTime + ") must be before endTime (" + endTime + ")"
                );
            }
        }
    }

    public record StaffServiceEligibilityFact(UUID staffId, UUID branchId, UUID serviceId) {
        public StaffServiceEligibilityFact {
            Objects.requireNonNull(staffId, "staffId must not be null");
            Objects.requireNonNull(branchId, "branchId must not be null");
            Objects.requireNonNull(serviceId, "serviceId must not be null");
        }
    }

    public record ExistingAppointmentFact(UUID appointmentId, UUID staffId, TimeInterval interval, AppointmentStatus status) {
        public ExistingAppointmentFact {
            Objects.requireNonNull(appointmentId, "appointmentId must not be null");
            Objects.requireNonNull(staffId, "staffId must not be null");
            Objects.requireNonNull(interval, "interval must not be null");
            Objects.requireNonNull(status, "status must not be null");
        }
    }

    private final UUID staffId;
    private final UUID branchId;
    private final UUID serviceId;
    private final TimeInterval requestedInterval;
    private final ZoneId branchTimeZone;
    private final List<BranchOpeningWindow> branchOpeningWindows;
    private final List<StaffWorkingWindow> staffWorkingWindows;
    private final Set<StaffServiceEligibilityFact> staffServiceEligibilities;
    private final List<ExistingAppointmentFact> existingAppointments;

    private BookingValidationContext(Builder builder) {
        this.staffId = Objects.requireNonNull(builder.staffId, "staffId must not be null");
        this.branchId = Objects.requireNonNull(builder.branchId, "branchId must not be null");
        this.serviceId = Objects.requireNonNull(builder.serviceId, "serviceId must not be null");
        this.requestedInterval = Objects.requireNonNull(builder.requestedInterval, "requestedInterval must not be null");
        this.branchTimeZone = Objects.requireNonNull(builder.branchTimeZone, "branchTimeZone must not be null");
        this.branchOpeningWindows = List.copyOf(builder.branchOpeningWindows);
        this.staffWorkingWindows = List.copyOf(builder.staffWorkingWindows);
        this.staffServiceEligibilities = Set.copyOf(builder.staffServiceEligibilities);
        this.existingAppointments = List.copyOf(builder.existingAppointments);
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID staffId() {
        return staffId;
    }

    public UUID branchId() {
        return branchId;
    }

    public UUID serviceId() {
        return serviceId;
    }

    public TimeInterval requestedInterval() {
        return requestedInterval;
    }

    public ZoneId branchTimeZone() {
        return branchTimeZone;
    }

    public List<BranchOpeningWindow> branchOpeningWindows() {
        return branchOpeningWindows;
    }

    public List<StaffWorkingWindow> staffWorkingWindows() {
        return staffWorkingWindows;
    }

    public Set<StaffServiceEligibilityFact> staffServiceEligibilities() {
        return staffServiceEligibilities;
    }

    public List<ExistingAppointmentFact> existingAppointments() {
        return existingAppointments;
    }

    public static final class Builder {
        private UUID staffId;
        private UUID branchId;
        private UUID serviceId;
        private TimeInterval requestedInterval;
        private ZoneId branchTimeZone = ZoneId.of("Europe/Istanbul");
        private final List<BranchOpeningWindow> branchOpeningWindows = new ArrayList<>();
        private final List<StaffWorkingWindow> staffWorkingWindows = new ArrayList<>();
        private final Set<StaffServiceEligibilityFact> staffServiceEligibilities = new HashSet<>();
        private final List<ExistingAppointmentFact> existingAppointments = new ArrayList<>();

        public Builder staffId(UUID staffId) {
            this.staffId = staffId;
            return this;
        }

        public Builder branchId(UUID branchId) {
            this.branchId = branchId;
            return this;
        }

        public Builder serviceId(UUID serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public Builder requestedInterval(TimeInterval requestedInterval) {
            this.requestedInterval = requestedInterval;
            return this;
        }

        public Builder branchTimeZone(ZoneId branchTimeZone) {
            this.branchTimeZone = branchTimeZone;
            return this;
        }

        public Builder addOpeningWindow(DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime) {
            this.branchOpeningWindows.add(new BranchOpeningWindow(dayOfWeek, openTime, closeTime));
            return this;
        }

        public Builder addOpeningWindow(BranchOpeningWindow window) {
            this.branchOpeningWindows.add(Objects.requireNonNull(window, "window must not be null"));
            return this;
        }

        public Builder addStaffWorkingWindow(UUID staffId, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
            this.staffWorkingWindows.add(new StaffWorkingWindow(staffId, dayOfWeek, startTime, endTime));
            return this;
        }

        public Builder addStaffWorkingWindow(StaffWorkingWindow window) {
            this.staffWorkingWindows.add(Objects.requireNonNull(window, "window must not be null"));
            return this;
        }

        public Builder addEligibility(UUID staffId, UUID branchId, UUID serviceId) {
            this.staffServiceEligibilities.add(new StaffServiceEligibilityFact(staffId, branchId, serviceId));
            return this;
        }

        public Builder addEligibility(StaffServiceEligibilityFact fact) {
            this.staffServiceEligibilities.add(Objects.requireNonNull(fact, "fact must not be null"));
            return this;
        }

        public Builder addExistingAppointment(UUID appointmentId, UUID staffId, TimeInterval interval, AppointmentStatus status) {
            this.existingAppointments.add(new ExistingAppointmentFact(appointmentId, staffId, interval, status));
            return this;
        }

        public Builder addExistingAppointment(ExistingAppointmentFact fact) {
            this.existingAppointments.add(Objects.requireNonNull(fact, "fact must not be null"));
            return this;
        }

        public BookingValidationContext build() {
            return new BookingValidationContext(this);
        }
    }
}
