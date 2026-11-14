package com.example.clinic.booking.application;

import com.example.clinic.booking.application.dto.BranchSummaryDto;
import com.example.clinic.booking.application.dto.ServiceSummaryDto;
import com.example.clinic.booking.application.dto.StaffSummaryDto;
import com.example.clinic.catalog.domain.BranchService;
import com.example.clinic.catalog.infrastructure.BranchServiceRepository;
import com.example.clinic.identity.domain.StaffProfile;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.infrastructure.BranchRepository;
import com.example.clinic.scheduling.application.AvailabilityCalculator;
import com.example.clinic.scheduling.domain.TimeSlot;
import com.example.clinic.staff.domain.StaffAssignment;
import com.example.clinic.staff.domain.StaffServiceEligibility;
import com.example.clinic.staff.infrastructure.StaffServiceEligibilityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Application query service orchestrating patient-facing catalogue lookups and availability calculation.
 * Ensures the web layer never directly queries persistence repositories.
 */
@Service
@Transactional(readOnly = true)
public class PatientBookingQueryService {

    private static final ZoneId DEFAULT_CLINIC_ZONE = ZoneId.of("Europe/Istanbul");

    private final BranchRepository branchRepository;
    private final BranchServiceRepository branchServiceRepository;
    private final StaffServiceEligibilityRepository staffServiceEligibilityRepository;
    private final AvailabilityCalculator availabilityCalculator;
    private final Clock clock;

    public PatientBookingQueryService(BranchRepository branchRepository,
                                     BranchServiceRepository branchServiceRepository,
                                     StaffServiceEligibilityRepository staffServiceEligibilityRepository,
                                     AvailabilityCalculator availabilityCalculator) {
        this(branchRepository, branchServiceRepository, staffServiceEligibilityRepository, availabilityCalculator, Clock.systemUTC());
    }

    @Autowired
    public PatientBookingQueryService(BranchRepository branchRepository,
                                     BranchServiceRepository branchServiceRepository,
                                     StaffServiceEligibilityRepository staffServiceEligibilityRepository,
                                     AvailabilityCalculator availabilityCalculator,
                                     Clock clock) {
        this.branchRepository = Objects.requireNonNull(branchRepository);
        this.branchServiceRepository = Objects.requireNonNull(branchServiceRepository);
        this.staffServiceEligibilityRepository = Objects.requireNonNull(staffServiceEligibilityRepository);
        this.availabilityCalculator = Objects.requireNonNull(availabilityCalculator);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Resolves today's calendar date in the selected branch's time zone (or Istanbul by default).
     */
    public LocalDate getDefaultBookingDate(UUID branchId) {
        ZoneId zone = DEFAULT_CLINIC_ZONE;
        if (branchId != null) {
            zone = branchRepository.findById(branchId)
                    .map(b -> ZoneId.of(b.getTimeZone()))
                    .orElse(DEFAULT_CLINIC_ZONE);
        }
        return LocalDate.now(clock.withZone(zone));
    }

    /**
     * Lists all active branches belonging to active organizations.
     */
    public List<BranchSummaryDto> getActiveBranches() {
        return branchRepository.findByActiveTrue().stream()
                .filter(b -> b.getOrganization().isActive())
                .map(b -> new BranchSummaryDto(b.getId(), b.getName(), b.getTimeZone(), b.getAddress()))
                .sorted(Comparator.comparing(BranchSummaryDto::name))
                .toList();
    }

    /**
     * Lists all active medical services offered by the specified active branch.
     */
    public List<ServiceSummaryDto> getActiveServicesForBranch(UUID branchId) {
        Objects.requireNonNull(branchId, "branchId must not be null");

        return branchServiceRepository.findByBranchIdAndActiveTrue(branchId).stream()
                .filter(BranchService::isActive)
                .filter(bs -> bs.getServiceDefinition().isActive())
                .filter(bs -> bs.getBranch().isActive() && bs.getBranch().getOrganization().isActive())
                .map(this::mapServiceSummary)
                .sorted(Comparator.comparing(ServiceSummaryDto::name))
                .toList();
    }

    /**
     * Lists all eligible, active medical staff members authorized to deliver the specified active branch service.
     */
    public List<StaffSummaryDto> getEligibleStaffForService(UUID branchServiceId) {
        Objects.requireNonNull(branchServiceId, "branchServiceId must not be null");

        List<StaffServiceEligibility> eligibilities =
                staffServiceEligibilityRepository.findByBranchServiceId(branchServiceId);

        return eligibilities.stream()
                .filter(e -> e.getBranchService().isActive()
                        && e.getBranchService().getServiceDefinition().isActive()
                        && e.getBranchService().getBranch().isActive()
                        && e.getBranchService().getBranch().getOrganization().isActive())
                .map(StaffServiceEligibility::getAssignment)
                .filter(StaffAssignment::isActive)
                .filter(a -> a.getDepartment().isActive())
                .map(StaffAssignment::getStaff)
                .filter(StaffProfile::isActive)
                .distinct()
                .map(s -> new StaffSummaryDto(
                        s.getId(),
                        s.getFullName(),
                        s.getSpecialty() != null ? s.getSpecialty() : "Genel",
                        s.getStaffType()
                ))
                .sorted(Comparator.comparing(StaffSummaryDto::fullName))
                .toList();
    }

    /**
     * Calculates available booking slot start times for the given clinician, service, and date.
     */
    public List<LocalTime> getAvailableSlotTimes(UUID staffId, UUID branchServiceId, LocalDate date) {
        if (staffId == null || branchServiceId == null || date == null) {
            return Collections.emptyList();
        }

        try {
            List<TimeSlot> slots = availabilityCalculator.calculateAvailableSlots(staffId, branchServiceId, date);
            return slots.stream()
                    .map(TimeSlot::startTime)
                    .sorted()
                    .toList();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Calculates full time slots (including start and end times) and verifies branch/date context.
     */
    public List<TimeSlot> getAvailableTimeSlots(UUID staffId, UUID branchServiceId, LocalDate date) {
        if (staffId == null || branchServiceId == null || date == null) {
            return Collections.emptyList();
        }
        return availabilityCalculator.calculateAvailableSlots(staffId, branchServiceId, date).stream()
                .sorted()
                .toList();
    }

    public Optional<BranchSummaryDto> getBranch(UUID branchId) {
        if (branchId == null) {
            return Optional.empty();
        }
        return branchRepository.findById(branchId)
                .filter(b -> b.isActive() && b.getOrganization().isActive())
                .map(b -> new BranchSummaryDto(b.getId(), b.getName(), b.getTimeZone(), b.getAddress()));
    }

    public Optional<ServiceSummaryDto> getService(UUID branchServiceId) {
        if (branchServiceId == null) {
            return Optional.empty();
        }
        return branchServiceRepository.findById(branchServiceId)
                .filter(bs -> bs.isActive()
                        && bs.getServiceDefinition().isActive()
                        && bs.getBranch().isActive()
                        && bs.getBranch().getOrganization().isActive())
                .map(this::mapServiceSummary);
    }

    private ServiceSummaryDto mapServiceSummary(BranchService bs) {
        return new ServiceSummaryDto(
                bs.getId(),
                bs.getServiceDefinition().getName(),
                bs.getServiceDefinition().getDescription(),
                bs.getDurationMinutes(),
                bs.getFeeAmount(),
                bs.getCurrency()
        );
    }
}
