package com.example.clinic.booking.application;

import com.example.clinic.booking.application.dto.AppointmentViewDto;
import com.example.clinic.booking.application.dto.WeeklyDayScheduleDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Dedicated application query service supplying presentation view projections for appointments.
 * Strictly respects multi-tenant authorization, patient ownership, and staff assignments.
 */
@Service
@Transactional(readOnly = true)
public class AppointmentQueryService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final ZoneId DEFAULT_CLINIC_ZONE = ZoneId.of("Europe/Istanbul");

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
     * Resolves the current clinic calendar date using the injected {@link Clock}.
     */
    public LocalDate getCurrentClinicDate() {
        return LocalDate.now(clock.withZone(DEFAULT_CLINIC_ZONE));
    }

    /**
     * Retrieves all appointments for the currently authenticated patient, sorted by start time descending.
     */
    public List<AppointmentViewDto> getPatientAppointmentsForCurrentUser() {
        PatientProfile patient = resolveRequiredPatientProfile();
        return getPatientAppointments(patient.getId());
    }

    /**
     * Retrieves a bounded page of appointments for the currently authenticated patient.
     */
    public Page<AppointmentViewDto> getPatientAppointmentsForCurrentUser(int page, int size) {
        PatientProfile patient = resolveRequiredPatientProfile();
        int safePage = Math.max(0, page);
        int safeSize = Math.clamp(size, 1, 50);
        Page<AppointmentJpaEntity> entityPage = appointmentJpaRepository.findByPatientIdOrderByStartAtUtcDesc(
                patient.getId(),
                PageRequest.of(safePage, safeSize)
        );
        return entityPage.map(this::mapEntityToDto);
    }

    /**
     * Retrieves all appointments for the specified patient ID.
     */
    public List<AppointmentViewDto> getPatientAppointments(UUID patientId) {
        Objects.requireNonNull(patientId, "patientId must not be null");
        List<AppointmentJpaEntity> entities = appointmentJpaRepository.findByPatientIdOrderByStartAtUtcDesc(patientId);
        return mapEntitiesToDtos(entities);
    }

    /**
     * Retrieves scheduled or pending appointments for the currently authenticated staff member.
     *
     * @param targetDate target date (if null, current clinic date is used)
     * @param view       "daily", "weekly", or "pending"
     */
    public List<AppointmentViewDto> getStaffAppointmentsForCurrentUser(LocalDate targetDate, String view) {
        UserAccount currentUser = securityContextFacade.getRequiredCurrentUser();
        StaffProfile staff = staffProfileRepository.findByUserAccountId(currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("Erişim reddedildi: Kullanıcıya ait hekim profili bulunamadı."));

        return getStaffAppointments(staff.getId(), targetDate, view);
    }

    /**
     * Retrieves scheduled or pending appointments for the specified staff member.
     * Filters daily and weekly ranges using each appointment's local branch timezone so cross-timezone
     * and late-evening/early-morning appointments never drop out due to UTC midnight truncation.
     */
    public List<AppointmentViewDto> getStaffAppointments(UUID staffId, LocalDate targetDate, String view) {
        Objects.requireNonNull(staffId, "staffId must not be null");

        if ("pending".equalsIgnoreCase(view)) {
            List<AppointmentJpaEntity> pendingEntities = appointmentJpaRepository
                    .findByStaffIdAndStatusOrderByStartAtUtcAsc(staffId, AppointmentStatus.PENDING);
            return mapEntitiesToDtos(pendingEntities);
        }

        LocalDate date = (targetDate != null) ? targetDate : getCurrentClinicDate();
        LocalDate rangeStart;
        LocalDate rangeEnd;

        if ("weekly".equalsIgnoreCase(view)) {
            rangeStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            rangeEnd = rangeStart.plusDays(6);
        } else {
            rangeStart = date;
            rangeEnd = date;
        }

        // Expand SQL UTC window by +/-1 day to safely encompass all branch time zones (-12..+14)
        Instant queryWindowStart = rangeStart.minusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant queryWindowEnd = rangeEnd.plusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<AppointmentJpaEntity> entities = appointmentJpaRepository.findByStaffIdAndWindow(
                staffId, queryWindowStart, queryWindowEnd);

        return mapEntitiesToDtos(entities).stream()
                .filter(dto -> !dto.localDate().isBefore(rangeStart) && !dto.localDate().isAfter(rangeEnd))
                .toList();
    }

    /**
     * Groups appointments across the 7 calendar days (Monday to Sunday) containing {@code referenceDate}.
     */
    public List<WeeklyDayScheduleDto> groupAppointmentsByWeekDays(LocalDate referenceDate, List<AppointmentViewDto> appointments) {
        LocalDate baseDate = (referenceDate != null) ? referenceDate : getCurrentClinicDate();
        LocalDate monday = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        Map<LocalDate, List<AppointmentViewDto>> byDate = (appointments != null ? appointments : Collections.<AppointmentViewDto>emptyList())
                .stream()
                .collect(Collectors.groupingBy(AppointmentViewDto::localDate));

        List<WeeklyDayScheduleDto> days = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate dayDate = monday.plusDays(i);
            List<AppointmentViewDto> dayAppointments = byDate.getOrDefault(dayDate, Collections.emptyList());
            days.add(new WeeklyDayScheduleDto(
                    dayDate,
                    turkishDayName(dayDate.getDayOfWeek()),
                    dayDate.format(DISPLAY_DATE_FORMATTER),
                    dayDate.equals(baseDate),
                    dayAppointments
            ));
        }
        return days;
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

    private PatientProfile resolveRequiredPatientProfile() {
        UserAccount currentUser = securityContextFacade.getRequiredCurrentUser();
        return patientProfileRepository.findByUserAccountId(currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("Erişim reddedildi: Kullanıcıya ait hasta profili bulunamadı."));
    }

    private List<AppointmentViewDto> mapEntitiesToDtos(List<AppointmentJpaEntity> entities) {
        if (entities.isEmpty()) {
            return Collections.emptyList();
        }

        Map<UUID, Branch> branchCache = new HashMap<>();
        Map<UUID, StaffProfile> staffCache = new HashMap<>();
        Map<UUID, PatientProfile> patientCache = new HashMap<>();

        Set<UUID> branchIds = entities.stream().map(AppointmentJpaEntity::getBranchId).collect(Collectors.toSet());
        Set<UUID> staffIds = entities.stream().map(AppointmentJpaEntity::getStaffId).collect(Collectors.toSet());
        Set<UUID> patientIds = entities.stream().map(AppointmentJpaEntity::getPatientId).collect(Collectors.toSet());

        branchRepository.findAllById(branchIds).forEach(b -> branchCache.put(b.getId(), b));
        staffProfileRepository.findAllById(staffIds).forEach(s -> staffCache.put(s.getId(), s));
        patientProfileRepository.findAllById(patientIds).forEach(p -> patientCache.put(p.getId(), p));

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
        boolean isPast = !now.isBefore(entity.getEndAtUtc());

        AppointmentStatus status = entity.getStatus();
        boolean canCancel = (status == AppointmentStatus.PENDING || status == AppointmentStatus.CONFIRMED)
                && now.isBefore(entity.getStartAtUtc());

        boolean canApprove = (status == AppointmentStatus.PENDING);
        boolean canReject = (status == AppointmentStatus.PENDING);
        boolean canComplete = (status == AppointmentStatus.CONFIRMED) && !now.isBefore(entity.getEndAtUtc());

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

    private String turkishDayName(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "Pazartesi";
            case TUESDAY -> "Salı";
            case WEDNESDAY -> "Çarşamba";
            case THURSDAY -> "Perşembe";
            case FRIDAY -> "Cuma";
            case SATURDAY -> "Cumartesi";
            case SUNDAY -> "Pazar";
        };
    }
}
