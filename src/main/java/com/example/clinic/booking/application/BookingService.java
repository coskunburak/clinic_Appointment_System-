package com.example.clinic.booking.application;

import com.example.clinic.booking.domain.Appointment;
import com.example.clinic.booking.domain.AppointmentStatus;
import com.example.clinic.booking.domain.ServiceSnapshot;
import com.example.clinic.booking.infrastructure.persistence.AppointmentJpaEntity;
import com.example.clinic.booking.infrastructure.persistence.AppointmentJpaRepository;
import com.example.clinic.booking.infrastructure.persistence.AppointmentStatusHistoryJpaEntity;
import com.example.clinic.booking.infrastructure.persistence.AppointmentStatusHistoryJpaRepository;
import com.example.clinic.catalog.domain.BranchService;
import com.example.clinic.catalog.infrastructure.BranchServiceRepository;
import com.example.clinic.common.domain.TimeInterval;
import com.example.clinic.common.security.SecurityContextFacade;
import com.example.clinic.identity.domain.PatientProfile;
import com.example.clinic.identity.domain.StaffProfile;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.PatientProfileRepository;
import com.example.clinic.identity.infrastructure.StaffProfileRepository;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.BranchOpeningHours;
import com.example.clinic.organization.infrastructure.BranchOpeningHoursRepository;
import com.example.clinic.organization.infrastructure.BranchRepository;
import com.example.clinic.staff.domain.AvailabilityException;
import com.example.clinic.staff.domain.AvailabilityExceptionKind;
import com.example.clinic.staff.domain.StaffAssignment;
import com.example.clinic.staff.domain.WeeklyAvailability;
import com.example.clinic.staff.infrastructure.AvailabilityExceptionRepository;
import com.example.clinic.staff.infrastructure.StaffAssignmentRepository;
import com.example.clinic.staff.infrastructure.StaffServiceEligibilityRepository;
import com.example.clinic.staff.infrastructure.WeeklyAvailabilityRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Production-level transactional appointment booking service implementing Sprint S06.
 * <p>
 * Key Invariants:
 * <ul>
 *   <li>Ordered pessimistic row locking: Patient row locked first, then Staff row (ADR-004, FR-029).</li>
 *   <li>Authoritative verification of branch, service, staff assignment, and service eligibility inside lock.</li>
 *   <li>Half-open {@code [start, end)} interval collision detection against blocking statuses (PENDING, CONFIRMED, COMPLETED).</li>
 *   <li>Immutable service snapshot preservation (ADR-005, FR-032).</li>
 *   <li>Daylight Saving Time (DST) gap and overlap validation in branch IANA zone (FR-033).</li>
 *   <li>Creation audit history event logging in same transaction (TC-017).</li>
 * </ul>
 */
@Service
public class BookingService {

    private record TimeRange(LocalTime start, LocalTime end) {
        TimeRange {
            Objects.requireNonNull(start, "start must not be null");
            Objects.requireNonNull(end, "end must not be null");
        }
    }

    private static final List<AppointmentStatus> BLOCKING_STATUSES = List.of(
            AppointmentStatus.PENDING,
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.COMPLETED
    );

    private final SecurityContextFacade securityContextFacade;
    private final PatientProfileRepository patientProfileRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final BranchRepository branchRepository;
    private final BranchServiceRepository branchServiceRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final StaffServiceEligibilityRepository staffServiceEligibilityRepository;
    private final BranchOpeningHoursRepository branchOpeningHoursRepository;
    private final WeeklyAvailabilityRepository weeklyAvailabilityRepository;
    private final AvailabilityExceptionRepository availabilityExceptionRepository;
    private final AppointmentJpaRepository appointmentJpaRepository;
    private final AppointmentStatusHistoryJpaRepository appointmentStatusHistoryJpaRepository;
    private final Clock clock;

    public BookingService(SecurityContextFacade securityContextFacade,
                          PatientProfileRepository patientProfileRepository,
                          StaffProfileRepository staffProfileRepository,
                          BranchRepository branchRepository,
                          BranchServiceRepository branchServiceRepository,
                          StaffAssignmentRepository staffAssignmentRepository,
                          StaffServiceEligibilityRepository staffServiceEligibilityRepository,
                          BranchOpeningHoursRepository branchOpeningHoursRepository,
                          WeeklyAvailabilityRepository weeklyAvailabilityRepository,
                          AvailabilityExceptionRepository availabilityExceptionRepository,
                          AppointmentJpaRepository appointmentJpaRepository,
                          AppointmentStatusHistoryJpaRepository appointmentStatusHistoryJpaRepository,
                          Clock clock) {
        this.securityContextFacade = securityContextFacade;
        this.patientProfileRepository = patientProfileRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.branchRepository = branchRepository;
        this.branchServiceRepository = branchServiceRepository;
        this.staffAssignmentRepository = staffAssignmentRepository;
        this.staffServiceEligibilityRepository = staffServiceEligibilityRepository;
        this.branchOpeningHoursRepository = branchOpeningHoursRepository;
        this.weeklyAvailabilityRepository = weeklyAvailabilityRepository;
        this.availabilityExceptionRepository = availabilityExceptionRepository;
        this.appointmentJpaRepository = appointmentJpaRepository;
        this.appointmentStatusHistoryJpaRepository = appointmentStatusHistoryJpaRepository;
        this.clock = clock;
    }

    /**
     * Executes appointment booking in a single database transaction with pessimistic locking.
     *
     * @param command validated booking command
     * @return persisted {@link Appointment} domain aggregate
     * @throws AppointmentConflictException if staff member has an overlapping blocking appointment
     * @throws PatientOverlapException     if patient has an overlapping blocking appointment (FR-029)
     * @throws BookingValidationException   if schedule, branch, staff, or DST validation fails
     * @throws AccessDeniedException        if authentication or patient ownership is violated
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Appointment bookAppointment(BookingCommand command) {
        Objects.requireNonNull(command, "BookingCommand must not be null");

        // 1. Resolve and verify authenticated actor
        UserAccount currentUser = securityContextFacade.getRequiredCurrentUser();
        PatientProfile patientProfile = patientProfileRepository.findByUserAccountId(currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("Access denied: current user does not have a patient profile"));

        // Impersonation defense: if patientId provided in payload, must match authenticated user's patient profile
        if (command.patientId() != null && !command.patientId().equals(patientProfile.getId())) {
            throw new AccessDeniedException("Access denied: cannot book appointment on behalf of another patient (" +
                    command.patientId() + ")");
        }
        UUID patientId = patientProfile.getId();

        // 2. Global Ordered Locking: Lock Patient first, then Staff (prevents deadlock)
        PatientProfile lockedPatient = patientProfileRepository.findByIdForUpdate(patientId)
                .orElseThrow(() -> new BookingValidationException("Patient profile not found: " + patientId));

        StaffProfile lockedStaff = staffProfileRepository.findByIdForUpdate(command.staffId())
                .orElseThrow(() -> new BookingValidationException("Staff profile not found: " + command.staffId()));

        if (!lockedStaff.isActive()) {
            throw new BookingValidationException("Staff member " + command.staffId() + " is inactive");
        }

        // 3. Authoritative verification of branch, service, assignment, and eligibility
        Branch branch = branchRepository.findById(command.branchId())
                .orElseThrow(() -> new BookingValidationException("Branch not found: " + command.branchId()));

        if (!branch.isActive()) {
            throw new BookingValidationException("Branch " + command.branchId() + " is inactive");
        }

        BranchService branchService = branchServiceRepository.findById(command.branchServiceId())
                .orElseThrow(() -> new BookingValidationException("Branch service not found: " + command.branchServiceId()));

        if (!branchService.isActive()) {
            throw new BookingValidationException("Branch service " + command.branchServiceId() + " is inactive");
        }

        // Cross-tenant mismatch defense
        if (!branchService.getBranch().getId().equals(branch.getId())) {
            throw new BookingValidationException("Branch service " + command.branchServiceId() +
                    " does not belong to branch " + command.branchId());
        }

        // Verify staff assignment to branch
        StaffAssignment assignment = staffAssignmentRepository
                .findByStaffIdAndBranchIdAndActiveTrue(command.staffId(), branch.getId())
                .orElseThrow(() -> new BookingValidationException("Staff member " + command.staffId() +
                        " is not actively assigned to branch " + branch.getId()));

        // Verify staff service eligibility
        boolean eligible = staffServiceEligibilityRepository.existsByAssignmentIdAndBranchServiceId(
                assignment.getId(), branchService.getId()
        );
        if (!eligible) {
            throw new BookingValidationException("Staff member " + command.staffId() +
                    " is not eligible to perform service " + branchService.getId() + " at branch " + branch.getId());
        }

        // 4. Branch Time Zone, DST, and UTC Conversion (FR-033)
        ZoneId branchZoneId = ZoneId.of(branch.getTimeZone());
        LocalDateTime localDateTime = LocalDateTime.of(command.appointmentDate(), command.startTime());
        ZoneRules zoneRules = branchZoneId.getRules();
        List<ZoneOffset> validOffsets = zoneRules.getValidOffsets(localDateTime);

        ZoneOffset selectedOffset;
        if (validOffsets.isEmpty()) {
            throw new BookingValidationException("Requested local time " + localDateTime +
                    " falls into a Daylight Saving Time gap (nonexistent time) in " + branchZoneId);
        } else if (validOffsets.size() == 1) {
            selectedOffset = validOffsets.get(0);
        } else {
            // DST Overlap (ambiguous time, e.g. Fall-back)
            if (command.zoneOffset() != null && validOffsets.contains(command.zoneOffset())) {
                selectedOffset = command.zoneOffset();
            } else {
                throw new BookingValidationException("Requested local time " + localDateTime +
                        " is ambiguous due to Daylight Saving Time overlap in " + branchZoneId +
                        "; valid offsets are: " + validOffsets);
            }
        }

        Instant startAtUtc = localDateTime.toInstant(selectedOffset);
        if (!startAtUtc.isAfter(clock.instant())) {
            throw new BookingValidationException("Appointment start time must be strictly in the future (requested: " +
                    startAtUtc + ", now: " + clock.instant() + ")");
        }

        int durationMinutes = branchService.getDurationMinutes();
        LocalTime reqStartTime = command.startTime();
        LocalTime reqEndTime;
        try {
            reqEndTime = reqStartTime.plusMinutes(durationMinutes);
        } catch (Exception e) {
            throw new BookingValidationException("Invalid appointment duration: " + durationMinutes + " minutes");
        }

        if (reqEndTime.isBefore(reqStartTime)) {
            throw new BookingValidationException("Appointment interval crosses midnight: " + reqStartTime + " - " + reqEndTime);
        }

        Instant endAtUtc = startAtUtc.plus(Duration.ofMinutes(durationMinutes));
        TimeInterval requestedInterval = TimeInterval.of(startAtUtc, endAtUtc);

        // 5. Verify Branch Opening Hours (FR-012, TC-012)
        DayOfWeek dayOfWeek = command.appointmentDate().getDayOfWeek();
        short weekday = (short) dayOfWeek.getValue();
        List<BranchOpeningHours> branchHours = branchOpeningHoursRepository.findByBranchIdAndWeekday(branch.getId(), weekday);
        if (branchHours.isEmpty()) {
            throw new BookingValidationException("Branch " + branch.getId() + " is closed on " + dayOfWeek);
        }

        boolean fitsInBranchHours = branchHours.stream().anyMatch(h ->
                !reqStartTime.isBefore(h.getStartTime()) && !reqEndTime.isAfter(h.getEndTime())
        );
        if (!fitsInBranchHours) {
            throw new BookingValidationException("Requested interval " + reqStartTime + " - " + reqEndTime +
                    " falls outside branch opening hours on " + dayOfWeek);
        }

        // 6. Verify Staff Weekly Availability & Exceptions (SCH-02, SCH-03)
        List<AvailabilityException> exceptions = availabilityExceptionRepository
                .findByAssignmentIdAndLocalDate(assignment.getId(), command.appointmentDate());

        boolean hasFullDayLeave = exceptions.stream()
                .anyMatch(e -> e.getKind() == AvailabilityExceptionKind.FULL_DAY_LEAVE);
        if (hasFullDayLeave) {
            throw new BookingValidationException("Staff member " + command.staffId() +
                    " is on full-day leave on " + command.appointmentDate());
        }

        List<WeeklyAvailability> weeklyAvailabilities = weeklyAvailabilityRepository
                .findByAssignmentIdAndWeekday(assignment.getId(), weekday);

        // Build effective working ranges
        List<TimeRange> workingRanges = new ArrayList<>();
        for (WeeklyAvailability weekly : weeklyAvailabilities) {
            workingRanges.add(new TimeRange(weekly.getStartTime(), weekly.getEndTime()));
        }

        // Include OVERTIME
        for (AvailabilityException ex : exceptions) {
            if (ex.getKind() == AvailabilityExceptionKind.OVERTIME &&
                    ex.getStartTime() != null && ex.getEndTime() != null) {
                workingRanges.add(new TimeRange(ex.getStartTime(), ex.getEndTime()));
            }
        }

        // Subtract PARTIAL_LEAVE
        for (AvailabilityException ex : exceptions) {
            if (ex.getKind() == AvailabilityExceptionKind.PARTIAL_LEAVE &&
                    ex.getStartTime() != null && ex.getEndTime() != null) {
                workingRanges = subtractRange(workingRanges, ex.getStartTime(), ex.getEndTime());
            }
        }

        boolean fitsInStaffHours = workingRanges.stream().anyMatch(r ->
                !reqStartTime.isBefore(r.start()) && !reqEndTime.isAfter(r.end())
        );
        if (!fitsInStaffHours) {
            throw new BookingValidationException("Staff member " + command.staffId() +
                    " is not available during requested interval " + reqStartTime + " - " + reqEndTime +
                    " on " + command.appointmentDate());
        }

        // 7. Authoritative Overlap Query for Staff (under staff lock, global across all branches)
        List<AppointmentJpaEntity> staffConflicts = appointmentJpaRepository.findOverlappingAppointmentsForStaff(
                command.staffId(),
                startAtUtc,
                endAtUtc,
                BLOCKING_STATUSES
        );
        if (!staffConflicts.isEmpty()) {
            AppointmentJpaEntity conflict = staffConflicts.get(0);
            throw new AppointmentConflictException("Staff member " + command.staffId() +
                    " already has a conflicting appointment (" + conflict.getId() + ") in status " +
                    conflict.getStatus() + " during interval [" + conflict.getStartAtUtc() + ", " +
                    conflict.getEndAtUtc() + ")");
        }

        // 8. Authoritative Overlap Query for Patient (under patient lock, global across all staff - FR-029)
        List<AppointmentJpaEntity> patientConflicts = appointmentJpaRepository.findOverlappingAppointmentsForPatient(
                patientId,
                startAtUtc,
                endAtUtc,
                BLOCKING_STATUSES
        );
        if (!patientConflicts.isEmpty()) {
            AppointmentJpaEntity conflict = patientConflicts.get(0);
            throw new PatientOverlapException("Patient " + patientId +
                    " already has a conflicting appointment (" + conflict.getId() + ") in status " +
                    conflict.getStatus() + " during interval [" + conflict.getStartAtUtc() + ", " +
                    conflict.getEndAtUtc() + ")");
        }

        // 9. Snapshot Creation (ADR-005, FR-032)
        ServiceSnapshot snapshot = branchService.toSnapshot();

        // 10. Persist Appointment (PENDING status) and Creation Audit Event (TC-017)
        Appointment appointment = Appointment.create(
                UUID.randomUUID(),
                patientId,
                command.staffId(),
                branch.getId(),
                branchService.getId(),
                requestedInterval,
                snapshot,
                clock.instant()
        );

        AppointmentJpaEntity entity = AppointmentJpaEntity.fromDomain(appointment, 0L);
        AppointmentJpaEntity savedEntity = appointmentJpaRepository.saveAndFlush(entity);

        AppointmentStatusHistoryJpaEntity historyEntity = new AppointmentStatusHistoryJpaEntity(
                UUID.randomUUID(),
                savedEntity.getId(),
                null, // from_status is null for initial creation event
                AppointmentStatus.PENDING,
                currentUser.getId(),
                "Initial appointment booking",
                clock.instant()
        );
        appointmentStatusHistoryJpaRepository.saveAndFlush(historyEntity);

        return savedEntity.toDomain();
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
            // Case 5: Leave cuts middle of range (split into two)
            if (leaveStart.isAfter(s) && leaveEnd.isBefore(e)) {
                result.add(new TimeRange(s, leaveStart));
                result.add(new TimeRange(leaveEnd, e));
            }
        }
        return result;
    }
}
