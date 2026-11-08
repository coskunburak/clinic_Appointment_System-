package com.example.clinic.scheduling.application;

import com.example.clinic.catalog.domain.BranchService;
import com.example.clinic.catalog.infrastructure.BranchServiceRepository;
import com.example.clinic.common.domain.TimeInterval;
import com.example.clinic.identity.domain.StaffProfile;
import com.example.clinic.identity.infrastructure.StaffProfileRepository;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.BranchOpeningHours;
import com.example.clinic.organization.infrastructure.BranchOpeningHoursRepository;
import com.example.clinic.scheduling.application.port.AppointmentSchedulePort;
import com.example.clinic.scheduling.domain.TimeSlot;
import com.example.clinic.staff.domain.AvailabilityException;
import com.example.clinic.staff.domain.AvailabilityExceptionKind;
import com.example.clinic.staff.domain.StaffAssignment;
import com.example.clinic.staff.domain.WeeklyAvailability;
import com.example.clinic.staff.infrastructure.AvailabilityExceptionRepository;
import com.example.clinic.staff.infrastructure.StaffAssignmentRepository;
import com.example.clinic.staff.infrastructure.StaffServiceEligibilityRepository;
import com.example.clinic.staff.infrastructure.WeeklyAvailabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Pure domain scheduling calculation engine implementing WP-S05-02.
 * Calculates available discrete booking slots for a medical staff member, service, and date,
 * strictly enforcing branch opening hours (SCH-01), weekly availability (SCH-02),
 * leave/overtime exceptions (SCH-03), service eligibility (STA-02), and existing appointments (ADR-003).
 */
@Service
@Transactional(readOnly = true)
public class AvailabilityCalculator {

    private record TimeRange(LocalTime start, LocalTime end) {
        TimeRange {
            Objects.requireNonNull(start, "start must not be null");
            Objects.requireNonNull(end, "end must not be null");
        }
    }

    private final StaffProfileRepository staffProfileRepository;
    private final BranchServiceRepository branchServiceRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final StaffServiceEligibilityRepository staffServiceEligibilityRepository;
    private final BranchOpeningHoursRepository branchOpeningHoursRepository;
    private final WeeklyAvailabilityRepository weeklyAvailabilityRepository;
    private final AvailabilityExceptionRepository availabilityExceptionRepository;
    private final AppointmentSchedulePort appointmentSchedulePort;
    private final Clock clock;

    public AvailabilityCalculator(StaffProfileRepository staffProfileRepository,
                                  BranchServiceRepository branchServiceRepository,
                                  StaffAssignmentRepository staffAssignmentRepository,
                                  StaffServiceEligibilityRepository staffServiceEligibilityRepository,
                                  BranchOpeningHoursRepository branchOpeningHoursRepository,
                                  WeeklyAvailabilityRepository weeklyAvailabilityRepository,
                                  AvailabilityExceptionRepository availabilityExceptionRepository,
                                  AppointmentSchedulePort appointmentSchedulePort,
                                  Clock clock) {
        this.staffProfileRepository = staffProfileRepository;
        this.branchServiceRepository = branchServiceRepository;
        this.staffAssignmentRepository = staffAssignmentRepository;
        this.staffServiceEligibilityRepository = staffServiceEligibilityRepository;
        this.branchOpeningHoursRepository = branchOpeningHoursRepository;
        this.weeklyAvailabilityRepository = weeklyAvailabilityRepository;
        this.availabilityExceptionRepository = availabilityExceptionRepository;
        this.appointmentSchedulePort = appointmentSchedulePort;
        this.clock = clock;
    }

    /**
     * Determines whether the specified staff member is assigned to the service's branch
     * and holds active eligibility to deliver the specified medical service.
     */
    public boolean isStaffEligible(UUID staffId, UUID branchServiceId) {
        Objects.requireNonNull(staffId, "staffId must not be null");
        Objects.requireNonNull(branchServiceId, "branchServiceId must not be null");

        Optional<BranchService> branchServiceOpt = branchServiceRepository.findById(branchServiceId);
        if (branchServiceOpt.isEmpty() || !branchServiceOpt.get().isActive()) {
            return false;
        }

        BranchService branchService = branchServiceOpt.get();
        UUID branchId = branchService.getBranch().getId();

        Optional<StaffAssignment> assignmentOpt =
                staffAssignmentRepository.findByStaffIdAndBranchIdAndActiveTrue(staffId, branchId);
        if (assignmentOpt.isEmpty()) {
            return false;
        }

        return staffServiceEligibilityRepository.existsByAssignmentIdAndBranchServiceId(
                assignmentOpt.get().getId(), branchServiceId
        );
    }

    /**
     * Calculates available booking slots for the given staff, service, and date using the
     * secondary {@link AppointmentSchedulePort} for existing appointment intervals.
     */
    public List<TimeSlot> calculateAvailableSlots(UUID staffId, UUID branchServiceId, LocalDate targetDate) {
        Objects.requireNonNull(staffId, "staffId must not be null");
        Objects.requireNonNull(branchServiceId, "branchServiceId must not be null");
        Objects.requireNonNull(targetDate, "targetDate must not be null");

        BranchService branchService = branchServiceRepository.findById(branchServiceId)
                .orElseThrow(() -> new IllegalArgumentException("BranchService not found: " + branchServiceId));

        ZoneId timeZone = ZoneId.of(branchService.getBranch().getTimeZone());
        List<TimeInterval> occupied = appointmentSchedulePort.findOccupiedIntervals(staffId, targetDate, timeZone);

        return calculateAvailableSlots(staffId, branchServiceId, targetDate, occupied);
    }

    /**
     * Core calculation engine overload accepting explicit existing appointment intervals.
     */
    public List<TimeSlot> calculateAvailableSlots(UUID staffId, UUID branchServiceId, LocalDate targetDate,
                                                  List<TimeInterval> existingAppointments) {
        Objects.requireNonNull(staffId, "staffId must not be null");
        Objects.requireNonNull(branchServiceId, "branchServiceId must not be null");
        Objects.requireNonNull(targetDate, "targetDate must not be null");

        // 1. Verify service and branch
        BranchService branchService = branchServiceRepository.findById(branchServiceId)
                .orElseThrow(() -> new IllegalArgumentException("BranchService not found: " + branchServiceId));

        if (!branchService.isActive()) {
            return Collections.emptyList();
        }

        Branch branch = branchService.getBranch();
        ZoneId branchTimeZone = ZoneId.of(branch.getTimeZone());
        int durationMinutes = branchService.getDurationMinutes();

        // 2. Verify date against current branch time (SCH-04, SCH-05)
        ZonedDateTime branchNow = clock.instant().atZone(branchTimeZone);
        LocalDate todayInBranch = branchNow.toLocalDate();
        LocalTime nowTimeInBranch = branchNow.toLocalTime();

        if (targetDate.isBefore(todayInBranch)) {
            return Collections.emptyList();
        }

        // 3. Verify staff and assignment
        StaffProfile staff = staffProfileRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("StaffProfile not found: " + staffId));

        if (!staff.isActive()) {
            return Collections.emptyList();
        }

        Optional<StaffAssignment> assignmentOpt =
                staffAssignmentRepository.findByStaffIdAndBranchIdAndActiveTrue(staffId, branch.getId());
        if (assignmentOpt.isEmpty()) {
            return Collections.emptyList();
        }
        StaffAssignment assignment = assignmentOpt.get();

        // 4. Verify service eligibility (FR-009, STA-02, TC-009)
        boolean eligible = staffServiceEligibilityRepository.existsByAssignmentIdAndBranchServiceId(
                assignment.getId(), branchServiceId
        );
        if (!eligible) {
            return Collections.emptyList();
        }

        // 5. Branch opening hours for day of week (SCH-01)
        DayOfWeek dayOfWeek = targetDate.getDayOfWeek();
        short weekday = (short) dayOfWeek.getValue();
        List<BranchOpeningHours> branchHours =
                branchOpeningHoursRepository.findByBranchIdAndWeekday(branch.getId(), weekday);
        if (branchHours.isEmpty()) {
            return Collections.emptyList();
        }

        // 6. Staff weekly availability (SCH-02, FR-010, TC-010)
        List<WeeklyAvailability> weeklyAvailabilities =
                weeklyAvailabilityRepository.findByAssignmentIdAndWeekday(assignment.getId(), weekday);
        if (weeklyAvailabilities.isEmpty()) {
            return Collections.emptyList();
        }

        // 7. Exceptions on target date (SCH-03, FR-011, TC-011)
        List<AvailabilityException> exceptions =
                availabilityExceptionRepository.findByAssignmentIdAndLocalDate(assignment.getId(), targetDate);

        // 7a. Full-day leave completely closes the day
        boolean hasFullDayLeave = exceptions.stream()
                .anyMatch(e -> e.getKind() == AvailabilityExceptionKind.FULL_DAY_LEAVE);
        if (hasFullDayLeave) {
            return Collections.emptyList();
        }

        // 8. Build initial working ranges intersecting staff weekly hours with branch opening hours
        List<TimeRange> workingRanges = new ArrayList<>();
        for (WeeklyAvailability weekly : weeklyAvailabilities) {
            LocalTime staffStart = weekly.getStartTime();
            LocalTime staffEnd = weekly.getEndTime();

            for (BranchOpeningHours bHours : branchHours) {
                LocalTime branchStart = bHours.getStartTime();
                LocalTime branchEnd = bHours.getEndTime();

                LocalTime overlapStart = staffStart.isAfter(branchStart) ? staffStart : branchStart;
                LocalTime overlapEnd = staffEnd.isBefore(branchEnd) ? staffEnd : branchEnd;

                if (overlapStart.isBefore(overlapEnd)) {
                    workingRanges.add(new TimeRange(overlapStart, overlapEnd));
                }
            }
        }

        // 8a. Include OVERTIME exceptions
        for (AvailabilityException exception : exceptions) {
            if (exception.getKind() == AvailabilityExceptionKind.OVERTIME &&
                    exception.getStartTime() != null && exception.getEndTime() != null) {
                LocalTime otStart = exception.getStartTime();
                LocalTime otEnd = exception.getEndTime();
                for (BranchOpeningHours bHours : branchHours) {
                    LocalTime overlapStart = otStart.isAfter(bHours.getStartTime()) ? otStart : bHours.getStartTime();
                    LocalTime overlapEnd = otEnd.isBefore(bHours.getEndTime()) ? otEnd : bHours.getEndTime();
                    if (overlapStart.isBefore(overlapEnd)) {
                        workingRanges.add(new TimeRange(overlapStart, overlapEnd));
                    }
                }
            }
        }

        // 8b. Subtract PARTIAL_LEAVE exceptions from working ranges
        for (AvailabilityException exception : exceptions) {
            if (exception.getKind() == AvailabilityExceptionKind.PARTIAL_LEAVE &&
                    exception.getStartTime() != null && exception.getEndTime() != null) {
                workingRanges = subtractRange(workingRanges, exception.getStartTime(), exception.getEndTime());
            }
        }

        if (workingRanges.isEmpty()) {
            return Collections.emptyList();
        }

        // 9. Generate discrete candidate slots using durationMinutes
        List<TimeSlot> candidateSlots = new ArrayList<>();
        for (TimeRange range : workingRanges) {
            LocalTime cursor = range.start();
            while (true) {
                // Check if cursor + duration exceeds range.end or rolls past midnight
                LocalTime slotEnd;
                try {
                    // Check rollover: if adding duration produces a time before cursor, it crossed midnight
                    slotEnd = cursor.plusMinutes(durationMinutes);
                } catch (Exception e) {
                    break;
                }

                if (slotEnd.isBefore(cursor) || slotEnd.isAfter(range.end())) {
                    break;
                }

                candidateSlots.add(new TimeSlot(cursor, slotEnd));
                cursor = slotEnd;
            }
        }

        // 10. Filter out slots overlapping with existing appointments (ADR-003, TC-011)
        List<TimeInterval> occupiedList = existingAppointments != null ? existingAppointments : Collections.emptyList();
        List<TimeRange> occupiedRanges = occupiedList.stream()
                .map(interval -> new TimeRange(
                        interval.startAt().atZone(branchTimeZone).toLocalTime(),
                        interval.endAt().atZone(branchTimeZone).toLocalTime()
                ))
                .toList();

        List<TimeSlot> freeSlots = candidateSlots.stream()
                .filter(slot -> occupiedRanges.stream().noneMatch(occ -> slot.overlaps(occ.start(), occ.end())))
                .toList();

        // 11. Filter out past slots when target date is today (SCH-04)
        if (targetDate.equals(todayInBranch)) {
            freeSlots = freeSlots.stream()
                    .filter(slot -> !slot.startTime().isBefore(nowTimeInBranch))
                    .toList();
        }

        return freeSlots.stream().sorted().distinct().toList();
    }

    private List<TimeRange> subtractRange(List<TimeRange> source, LocalTime leaveStart, LocalTime leaveEnd) {
        List<TimeRange> result = new ArrayList<>();
        for (TimeRange range : source) {
            LocalTime s = range.start();
            LocalTime e = range.end();

            // Case 1: No overlap
            if (!leaveStart.isBefore(e) || !leaveEnd.isAfter(s)) {
                result.add(range);
                continue;
            }

            // Case 2: Leave completely covers range
            if (!leaveStart.isAfter(s) && !leaveEnd.isBefore(e)) {
                continue;
            }

            // Case 3: Leave cuts start of range
            if (!leaveStart.isAfter(s) && leaveEnd.isBefore(e)) {
                result.add(new TimeRange(leaveEnd, e));
                continue;
            }

            // Case 4: Leave cuts end of range
            if (leaveStart.isAfter(s) && !leaveEnd.isBefore(e)) {
                result.add(new TimeRange(s, leaveStart));
                continue;
            }

            // Case 5: Leave is in the middle of range (split into two)
            if (leaveStart.isAfter(s) && leaveEnd.isBefore(e)) {
                result.add(new TimeRange(s, leaveStart));
                result.add(new TimeRange(leaveEnd, e));
            }
        }
        return result;
    }
}
