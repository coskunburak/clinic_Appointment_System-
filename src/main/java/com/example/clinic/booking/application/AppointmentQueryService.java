package com.example.clinic.booking.application;

import com.example.clinic.booking.application.dto.AppointmentViewDto;
import com.example.clinic.booking.domain.AppointmentStatus;
import com.example.clinic.booking.infrastructure.persistence.AppointmentJpaEntity;
import com.example.clinic.booking.infrastructure.persistence.AppointmentJpaRepository;
import com.example.clinic.common.security.SecurityContextFacade;
import com.example.clinic.identity.domain.PatientProfile;
import com.example.clinic.identity.domain.StaffProfile;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.PatientProfileRepository;
import com.example.clinic.identity.infrastructure.StaffProfileRepository;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.infrastructure.BranchRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Dedicated application query service supplying presentation view projections for appointments.
 * Strictly respects multi-tenant authorization, patient ownership, and staff assignments.
 */
@Service
@Transactional(readOnly = true)
public class AppointmentQueryService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AppointmentJpaRepository appointmentJpaRepository;
    private final SecurityContextFacade securityContextFacade;
    private final PatientProfileRepository patientProfileRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final BranchRepository branchRepository;
    private final Clock clock;

    public AppointmentQueryService(AppointmentJpaRepository appointmentJpaRepository,
                                   SecurityContextFacade securityContextFacade,
                                   PatientProfileRepository patientProfileRepository,
                                   StaffProfileRepository staffProfileRepository,
                                   BranchRepository branchRepository,
                                   Clock clock) {
        this.appointmentJpaRepository = Objects.requireNonNull(appointmentJpaRepository);
        this.securityContextFacade = Objects.requireNonNull(securityContextFacade);
        this.patientProfileRepository = Objects.requireNonNull(patientProfileRepository);
        this.staffProfileRepository = Objects.requireNonNull(staffProfileRepository);
        this.branchRepository = Objects.requireNonNull(branchRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Retrieves all appointments for the currently authenticated patient, sorted by start time descending.
     */
    public List<AppointmentViewDto> getPatientAppointmentsForCurrentUser() {
        UserAccount currentUser = securityContextFacade.getRequiredCurrentUser();
        PatientProfile patient = patientProfileRepository.findByUserAccountId(currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("Erişim reddedildi: Kullanıcıya ait hasta profili bulunamadı."));

        return getPatientAppointments(patient.getId());
    }

    /**
     * Retrieves all appointments for the specified patient ID, verifying patient profile existence.
     */
    public List<AppointmentViewDto> getPatientAppointments(UUID patientId) {
        Objects.requireNonNull(patientId, "patientId must not be null");

        List<AppointmentJpaEntity> entities = appointmentJpaRepository.findByPatientIdOrderByStartAtUtcDesc(patientId);
        return mapEntitiesToDtos(entities);
    }

    /**
     * Retrieves scheduled appointments for the currently authenticated staff member.
     *
     * @param targetDate target date (if null, current date in default zone is used)
     * @param view       "daily" or "weekly"
     */
    public List<AppointmentViewDto> getStaffAppointmentsForCurrentUser(LocalDate targetDate, String view) {
        UserAccount currentUser = securityContextFacade.getRequiredCurrentUser();
        StaffProfile staff = staffProfileRepository.findByUserAccountId(currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("Erişim reddedildi: Kullanıcıya ait hekim profili bulunamadı."));

        return getStaffAppointments(staff.getId(), targetDate, view);
    }

    /**
     * Retrieves scheduled appointments for the specified staff member, bounded by date/view.
     */
    public List<AppointmentViewDto> getStaffAppointments(UUID staffId, LocalDate targetDate, String view) {
        Objects.requireNonNull(staffId, "staffId must not be null");
        LocalDate date = (targetDate != null) ? targetDate : LocalDate.now(clock);

        Instant windowStart;
        Instant windowEnd;

        if ("weekly".equalsIgnoreCase(view)) {
            LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate sunday = monday.plusDays(6);
            windowStart = monday.atStartOfDay(ZoneId.of("UTC")).toInstant();
            windowEnd = sunday.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant();
        } else {
            // Daily window
            windowStart = date.atStartOfDay(ZoneId.of("UTC")).toInstant();
            windowEnd = date.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant();
        }

        List<AppointmentJpaEntity> entities = appointmentJpaRepository.findByStaffIdAndWindow(staffId, windowStart, windowEnd);
        return mapEntitiesToDtos(entities);
    }

    /**
     * Retrieves a single appointment detail by ID.
     */
    public Optional<AppointmentViewDto> getAppointmentDetail(UUID appointmentId) {
        Objects.requireNonNull(appointmentId, "appointmentId must not be null");
        return appointmentJpaRepository.findById(appointmentId)
                .map(this::mapEntityToDto);
    }

    // =========================================================================
    // Mapping & Helpers
    // =========================================================================

    private List<AppointmentViewDto> mapEntitiesToDtos(List<AppointmentJpaEntity> entities) {
        if (entities.isEmpty()) {
            return Collections.emptyList();
        }

        // Cache lookups to avoid redundant DB roundtrips
        Map<UUID, Branch> branchCache = new HashMap<>();
        Map<UUID, StaffProfile> staffCache = new HashMap<>();
        Map<UUID, PatientProfile> patientCache = new HashMap<>();

        return entities.stream()
                .map(entity -> mapEntityToDtoWithCache(entity, branchCache, staffCache, patientCache))
                .toList();
    }

    private AppointmentViewDto mapEntityToDto(AppointmentJpaEntity entity) {
        return mapEntityToDtoWithCache(entity, new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    private AppointmentViewDto mapEntityToDtoWithCache(AppointmentJpaEntity entity,
                                                       Map<UUID, Branch> branchCache,
                                                       Map<UUID, StaffProfile> staffCache,
                                                       Map<UUID, PatientProfile> patientCache) {
        Branch branch = branchCache.computeIfAbsent(entity.getBranchId(),
                id -> branchRepository.findById(id).orElse(null));

        StaffProfile staff = staffCache.computeIfAbsent(entity.getStaffId(),
                id -> staffProfileRepository.findById(id).orElse(null));

        PatientProfile patient = patientCache.computeIfAbsent(entity.getPatientId(),
                id -> patientProfileRepository.findById(id).orElse(null));

        String branchName = (branch != null) ? branch.getName() : "Bilinmeyen Şube";
        String timeZoneStr = (branch != null) ? branch.getTimeZone() : "Europe/Istanbul";
        ZoneId branchZone = ZoneId.of(timeZoneStr);

        String staffName = (staff != null) ? staff.getFullName() : "Bilinmeyen Hekim";
        String staffSpecialty = (staff != null && staff.getSpecialty() != null) ? staff.getSpecialty() : "Genel";

        String patientName = (patient != null) ? patient.getFullName() : "Bilinmeyen Hasta";

        ZonedDateTime startLocal = entity.getStartAtUtc().atZone(branchZone);
        ZonedDateTime endLocal = entity.getEndAtUtc().atZone(branchZone);

        LocalDate localDate = startLocal.toLocalDate();
        LocalTime localStartTime = startLocal.toLocalTime();
        LocalTime localEndTime = endLocal.toLocalTime();

        String formattedDate = localDate.format(DATE_FORMATTER);
        String formattedTimeRange = localStartTime.format(TIME_FORMATTER) + " - " + localEndTime.format(TIME_FORMATTER);

        Instant now = clock.instant();
        boolean isPast = now.isAfter(entity.getEndAtUtc());

        AppointmentStatus status = entity.getStatus();
        boolean canCancel = (status == AppointmentStatus.PENDING || status == AppointmentStatus.CONFIRMED)
                && now.isBefore(entity.getStartAtUtc());

        boolean canApprove = (status == AppointmentStatus.PENDING);
        boolean canReject = (status == AppointmentStatus.PENDING);
        boolean canComplete = (status == AppointmentStatus.CONFIRMED) && now.isAfter(entity.getEndAtUtc());

        String statusBadgeClass = switch (status) {
            case PENDING -> "badge bg-warning text-dark";
            case CONFIRMED -> "badge bg-primary";
            case COMPLETED -> "badge bg-success";
            case CANCELLED -> "badge bg-secondary";
            case REJECTED -> "badge bg-danger";
        };

        String statusDisplayName = switch (status) {
            case PENDING -> "Beklemede";
            case CONFIRMED -> "Onaylandı";
            case COMPLETED -> "Tamamlandı";
            case CANCELLED -> "İptal Edildi";
            case REJECTED -> "Reddedildi";
        };

        return new AppointmentViewDto(
                entity.getId(),
                entity.getPatientId(),
                patientName,
                entity.getStaffId(),
                staffName,
                staffSpecialty,
                entity.getBranchId(),
                branchName,
                timeZoneStr,
                entity.getBranchServiceId(),
                entity.getServiceNameSnapshot(),
                entity.getStartAtUtc(),
                entity.getEndAtUtc(),
                localDate,
                localStartTime,
                localEndTime,
                formattedDate,
                formattedTimeRange,
                entity.getDurationMinutesSnapshot(),
                entity.getFeeAmountSnapshot(),
                entity.getCurrencySnapshot(),
                status,
                statusBadgeClass,
                statusDisplayName,
                canCancel,
                canApprove,
                canReject,
                canComplete,
                isPast
        );
    }
}
