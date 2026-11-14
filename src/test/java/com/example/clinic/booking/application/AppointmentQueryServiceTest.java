package com.example.clinic.booking.application;

import com.example.clinic.booking.application.dto.AppointmentViewDto;
import com.example.clinic.booking.application.dto.WeeklyDayScheduleDto;
import com.example.clinic.booking.domain.AppointmentStatus;
import com.example.clinic.booking.infrastructure.persistence.AppointmentJpaEntity;
import com.example.clinic.booking.infrastructure.persistence.AppointmentJpaRepository;
import com.example.clinic.common.security.SecurityContextFacade;
import com.example.clinic.identity.domain.PatientProfile;
import com.example.clinic.identity.domain.StaffProfile;
import com.example.clinic.identity.domain.StaffType;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.PatientProfileRepository;
import com.example.clinic.identity.infrastructure.StaffProfileRepository;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.Organization;
import com.example.clinic.organization.infrastructure.BranchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Isolated unit tests for AppointmentQueryService business projection logic.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentQueryServiceTest {

    @Mock
    private AppointmentJpaRepository appointmentJpaRepository;

    @Mock
    private SecurityContextFacade securityContextFacade;

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @Mock
    private StaffProfileRepository staffProfileRepository;

    @Mock
    private BranchRepository branchRepository;

    private Clock fixedClock;
    private AppointmentQueryService service;

    private UserAccount patientUser;
    private PatientProfile patientProfile;
    private Branch branch;
    private StaffProfile staffProfile;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneId.of("UTC"));
        service = new AppointmentQueryService(
                appointmentJpaRepository,
                securityContextFacade,
                patientProfileRepository,
                staffProfileRepository,
                branchRepository,
                fixedClock
        );

        patientUser = new UserAccount("patient@clinic.test", "hash");
        patientProfile = new PatientProfile(patientUser, "Ayşe Demir", "5551112233");

        Organization org = new Organization("Test Hospital");
        branch = new Branch(org, "Merkez Şube", "Europe/Istanbul", "İstanbul");

        UserAccount docUser = new UserAccount("doc@clinic.test", "hash");
        staffProfile = new StaffProfile(docUser, "Dr. Ali Kaya", "5554443322", StaffType.DOCTOR, "Kardiyoloji");
    }

    @Test
    @DisplayName("getPatientAppointmentsForCurrentUser maps UTC instants to branch timezone correctly")
    void mapsInstantsToBranchTimeZone() {
        when(securityContextFacade.getRequiredCurrentUser()).thenReturn(patientUser);
        when(patientProfileRepository.findByUserAccountId(patientUser.getId())).thenReturn(Optional.of(patientProfile));

        UUID appId = UUID.randomUUID();
        // 2026-11-20T11:00:00Z -> Europe/Istanbul (+03:00) is 14:00
        Instant startUtc = Instant.parse("2026-11-20T11:00:00Z");
        Instant endUtc = Instant.parse("2026-11-20T11:30:00Z");

        AppointmentJpaEntity entity = new AppointmentJpaEntity(
                appId, patientProfile.getId(), staffProfile.getId(), branch.getId(), UUID.randomUUID(),
                startUtc, endUtc, AppointmentStatus.PENDING, "Genel Muayene", 30,
                BigDecimal.valueOf(500), "TRY", 0L, Instant.now()
        );

        when(appointmentJpaRepository.findByPatientIdOrderByStartAtUtcDesc(patientProfile.getId()))
                .thenReturn(List.of(entity));
        when(branchRepository.findById(branch.getId())).thenReturn(Optional.of(branch));
        when(staffProfileRepository.findById(staffProfile.getId())).thenReturn(Optional.of(staffProfile));
        when(patientProfileRepository.findById(patientProfile.getId())).thenReturn(Optional.of(patientProfile));

        List<AppointmentViewDto> result = service.getPatientAppointmentsForCurrentUser();

        assertThat(result).hasSize(1);
        AppointmentViewDto dto = result.get(0);
        assertThat(dto.patientName()).isEqualTo("Ayşe Demir");
        assertThat(dto.staffName()).isEqualTo("Dr. Ali Kaya");
        assertThat(dto.branchName()).isEqualTo("Merkez Şube");
        assertThat(dto.localDate()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(dto.localStartTime()).isEqualTo(java.time.LocalTime.of(14, 0));
        assertThat(dto.localEndTime()).isEqualTo(java.time.LocalTime.of(14, 30));
        assertThat(dto.canCancel()).isTrue();
        assertThat(dto.canApprove()).isTrue();
        assertThat(dto.isPast()).isFalse();
    }

    @Test
    @DisplayName("Throws AccessDeniedException if authenticated user does not have a patient profile")
    void throwsAccessDeniedWhenPatientProfileMissing() {
        when(securityContextFacade.getRequiredCurrentUser()).thenReturn(patientUser);
        when(patientProfileRepository.findByUserAccountId(patientUser.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPatientAppointmentsForCurrentUser())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("hasta profili bulunamadı");
    }

    @Test
    @DisplayName("Daily staff schedule includes early-morning branch local appointments across UTC midnight and sets exact canComplete boundary")
    void dailyStaffScheduleRespectsBranchTimeZoneAndCanCompleteBoundary() {
        UserAccount docUser = staffProfile.getUserAccount();
        when(securityContextFacade.getRequiredCurrentUser()).thenReturn(docUser);
        when(staffProfileRepository.findByUserAccountId(docUser.getId())).thenReturn(Optional.of(staffProfile));

        // 2026-11-19T22:00:00Z == 2026-11-20T01:00:00+03:00 in Europe/Istanbul (fixedClock is 2026-11-20T10:00:00Z)
        AppointmentJpaEntity earlyLocal = new AppointmentJpaEntity(
                UUID.randomUUID(),
                patientProfile.getId(),
                staffProfile.getId(),
                branch.getId(),
                UUID.randomUUID(),
                Instant.parse("2026-11-19T22:00:00Z"),
                Instant.parse("2026-11-19T22:30:00Z"),
                AppointmentStatus.CONFIRMED,
                "Sabah Kontrolü",
                30,
                new BigDecimal("400.00"),
                "TRY",
                0L,
                Instant.now()
        );
        // Appointment ending at exact fixedClock instant (10:00:00Z) -> canComplete MUST be true
        AppointmentJpaEntity exactEnd = new AppointmentJpaEntity(
                UUID.randomUUID(),
                patientProfile.getId(),
                staffProfile.getId(),
                branch.getId(),
                UUID.randomUUID(),
                Instant.parse("2026-11-20T09:30:00Z"),
                Instant.parse("2026-11-20T10:00:00Z"),
                AppointmentStatus.CONFIRMED,
                "Öğle Kontrolü",
                30,
                new BigDecimal("400.00"),
                "TRY",
                0L,
                Instant.now()
        );
        // Previous local day in Istanbul: 2026-11-19T20:00:00Z == 2026-11-19T23:00:00+03:00 -> must be filtered out
        AppointmentJpaEntity previousLocalDay = new AppointmentJpaEntity(
                UUID.randomUUID(),
                patientProfile.getId(),
                staffProfile.getId(),
                branch.getId(),
                UUID.randomUUID(),
                Instant.parse("2026-11-19T20:00:00Z"),
                Instant.parse("2026-11-19T20:30:00Z"),
                AppointmentStatus.CONFIRMED,
                "Önceki Gün",
                30,
                new BigDecimal("400.00"),
                "TRY",
                0L,
                Instant.now()
        );

        when(appointmentJpaRepository.findByStaffIdAndWindow(eq(staffProfile.getId()), any(), any()))
                .thenReturn(List.of(previousLocalDay, earlyLocal, exactEnd));
        when(branchRepository.findAllById(any())).thenReturn(List.of(branch));
        when(staffProfileRepository.findAllById(any())).thenReturn(List.of(staffProfile));
        when(patientProfileRepository.findAllById(any())).thenReturn(List.of(patientProfile));

        List<AppointmentViewDto> result = service.getStaffAppointmentsForCurrentUser(LocalDate.of(2026, 11, 20), "daily");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).serviceName()).isEqualTo("Sabah Kontrolü");
        assertThat(result.get(0).localDate()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(result.get(0).canComplete()).isTrue();
        assertThat(result.get(1).serviceName()).isEqualTo("Öğle Kontrolü");
        assertThat(result.get(1).canComplete()).isTrue();
        assertThat(result.get(1).isPast()).isTrue();

        List<WeeklyDayScheduleDto> weekDays = service.groupAppointmentsByWeekDays(LocalDate.of(2026, 11, 20), result);
        assertThat(weekDays).hasSize(7);
        assertThat(weekDays.get(4).selected()).isTrue();
        assertThat(weekDays.get(4).appointments()).hasSize(2);
    }

    @Test
    @DisplayName("Pending staff view queries all PENDING appointments across dates")
    void pendingStaffViewQueriesPendingStatus() {
        UserAccount docUser = staffProfile.getUserAccount();
        when(securityContextFacade.getRequiredCurrentUser()).thenReturn(docUser);
        when(staffProfileRepository.findByUserAccountId(docUser.getId())).thenReturn(Optional.of(staffProfile));
        when(appointmentJpaRepository.findByStaffIdAndStatusOrderByStartAtUtcAsc(staffProfile.getId(), AppointmentStatus.PENDING))
                .thenReturn(List.of());

        List<AppointmentViewDto> pending = service.getStaffAppointmentsForCurrentUser(LocalDate.of(2026, 11, 20), "pending");
        assertThat(pending).isEmpty();
    }
}
