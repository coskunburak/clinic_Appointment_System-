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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Application query service orchestrating patient-facing catalogue lookups and availability calculation.
 * Ensures the web layer never directly queries persistence repositories (Section 2.3, 3.1).
 */
@Service
@Transactional(readOnly = true)
public class PatientBookingQueryService {

    private final BranchRepository branchRepository;
    private final BranchServiceRepository branchServiceRepository;
    private final StaffServiceEligibilityRepository staffServiceEligibilityRepository;
    private final AvailabilityCalculator availabilityCalculator;

    public PatientBookingQueryService(BranchRepository branchRepository,
                                     BranchServiceRepository branchServiceRepository,
                                     StaffServiceEligibilityRepository staffServiceEligibilityRepository,
                                     AvailabilityCalculator availabilityCalculator) {
        this.branchRepository = Objects.requireNonNull(branchRepository);
        this.branchServiceRepository = Objects.requireNonNull(branchServiceRepository);
        this.staffServiceEligibilityRepository = Objects.requireNonNull(staffServiceEligibilityRepository);
        this.availabilityCalculator = Objects.requireNonNull(availabilityCalculator);
    }

    /**
     * Lists all active branches available for appointment booking.
     */
    public List<BranchSummaryDto> getActiveBranches() {
        return branchRepository.findByActiveTrue().stream()
                .filter(b -> b.getOrganization().isActive())
                .map(b -> new BranchSummaryDto(b.getId(), b.getName(), b.getTimeZone(), b.getAddress()))
                .sorted(Comparator.comparing(BranchSummaryDto::name))
                .toList();
    }

    /**
     * Lists all active medical services offered by the specified branch.
     */
    public List<ServiceSummaryDto> getActiveServicesForBranch(UUID branchId) {
        Objects.requireNonNull(branchId, "branchId must not be null");

        return branchServiceRepository.findByBranchIdAndActiveTrue(branchId).stream()
                .filter(bs -> bs.getServiceDefinition().isActive())
                .map(bs -> new ServiceSummaryDto(
                        bs.getId(),
                        bs.getServiceDefinition().getName(),
                        bs.getServiceDefinition().getDescription(),
                        bs.getDurationMinutes(),
                        bs.getFeeAmount(),
                        bs.getCurrency()
                ))
                .sorted(Comparator.comparing(ServiceSummaryDto::name))
                .toList();
    }

    /**
     * Lists all eligible, active medical staff members authorized to deliver the specified service.
     */
    public List<StaffSummaryDto> getEligibleStaffForService(UUID branchServiceId) {
        Objects.requireNonNull(branchServiceId, "branchServiceId must not be null");

        List<StaffServiceEligibility> eligibilities =
                staffServiceEligibilityRepository.findByBranchServiceId(branchServiceId);

        return eligibilities.stream()
                .map(StaffServiceEligibility::getAssignment)
                .filter(StaffAssignment::isActive)
                .map(StaffAssignment::getStaff)
                .filter(StaffProfile::isActive)
                .distinct()
                .map(s -> new StaffSummaryDto(
                        s.getId(),
                        s.getFullName(),
                        s.getSpecialty() != null ? s.getSpecialty() : "Genel",
                        s.getStaffType().name()
                ))
                .sorted(Comparator.comparing(StaffSummaryDto::fullName))
                .toList();
    }

    /**
     * Calculates available booking slots for the given clinician, service, and date.
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
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public Optional<BranchSummaryDto> getBranch(UUID branchId) {
        return branchRepository.findById(branchId)
                .map(b -> new BranchSummaryDto(b.getId(), b.getName(), b.getTimeZone(), b.getAddress()));
    }

    public Optional<ServiceSummaryDto> getService(UUID branchServiceId) {
        return branchServiceRepository.findById(branchServiceId)
                .map(bs -> new ServiceSummaryDto(
                        bs.getId(),
                        bs.getServiceDefinition().getName(),
                        bs.getServiceDefinition().getDescription(),
                        bs.getDurationMinutes(),
                        bs.getFeeAmount(),
                        bs.getCurrency()
                ));
    }
}
