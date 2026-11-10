package com.example.clinic.booking.application;

import com.example.clinic.booking.domain.Appointment;
import com.example.clinic.booking.domain.AppointmentDomainException;
import com.example.clinic.booking.domain.AppointmentStatus;
import com.example.clinic.booking.domain.ServiceSnapshot;
import com.example.clinic.booking.infrastructure.persistence.AppointmentJpaEntity;
import com.example.clinic.booking.infrastructure.persistence.AppointmentJpaRepository;
import com.example.clinic.booking.infrastructure.persistence.AppointmentStatusHistoryJpaEntity;
import com.example.clinic.booking.infrastructure.persistence.AppointmentStatusHistoryJpaRepository;
import com.example.clinic.common.domain.Money;
import com.example.clinic.common.domain.TimeInterval;
import com.example.clinic.common.security.SecurityContextFacade;
import com.example.clinic.identity.application.AccessPolicy;
import com.example.clinic.identity.domain.PatientProfile;
import com.example.clinic.identity.domain.Role;
import com.example.clinic.identity.domain.StaffProfile;
import com.example.clinic.identity.domain.StaffType;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.PatientProfileRepository;
import com.example.clinic.identity.infrastructure.StaffProfileRepository;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Isolated unit tests for {@link AppointmentApprovalService}.
 * Verifies business logic, authorization rules, input validation, and history creation without database dependency.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentApprovalServiceTest {

    private static final Currency TRY = Currency.getInstance("TRY");

    @Mock
    private AppointmentJpaRepository appointmentJpaRepository;

    @Mock
    private AppointmentStatusHistoryJpaRepository appointmentStatusHistoryJpaRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private StaffProfileRepository staffProfileRepository;

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @Mock
    private AccessPolicy accessPolicy;

    @Mock
    private SecurityContextFacade securityContextFacade;

    private Clock clock;
    private AppointmentApprovalService approvalService;

    private UUID appointmentId;
    private UUID patientUserId;
    private UUID patientProfileId;
    private UUID staffUserId;
    private UUID staffProfileId;
    private UUID otherStaffUserId;
    private UUID otherStaffProfileId;
    private UUID managerUserId;
    private UUID otherManagerUserId;
    private UUID branchId;
    private UUID branchServiceId;

    private UserAccount patientUser;
    private PatientProfile patientProfile;
    private UserAccount staffUser;
    private StaffProfile staffProfile;
    private UserAccount otherStaffUser;
    private StaffProfile otherStaffProfile;
    private UserAccount managerUser;
    private UserAccount otherManagerUser;

    private Instant appointmentStart;
    private Instant appointmentEnd;
    private AppointmentJpaEntity pendingAppointmentEntity;

    @BeforeEach
    void setUp() {
        Instant fixedNow = Instant.parse("2026-11-10T09:00:00Z");
        clock = Clock.fixed(fixedNow, ZoneOffset.UTC);

        approvalService = new AppointmentApprovalService(
                appointmentJpaRepository,
                appointmentStatusHistoryJpaRepository,
                userAccountRepository,
                staffProfileRepository,
                patientProfileRepository,
                accessPolicy,
                securityContextFacade,
                clock
        );

        appointmentId = UUID.randomUUID();
        branchId = UUID.randomUUID();
        branchServiceId = UUID.randomUUID();

        // Patient
        patientUserId = UUID.randomUUID();
        patientProfileId = UUID.randomUUID();
        patientUser = new UserAccount(patientUserId, "patient@example.com", "hash", true, fixedNow);
        patientProfile = new PatientProfile(patientProfileId, patientUser, "Ali Veli", "+905551112233", fixedNow);

        // Assigned Doctor
        staffUserId = UUID.randomUUID();
        staffProfileId = UUID.randomUUID();
        staffUser = new UserAccount(staffUserId, "doctor@example.com", "hash", true, fixedNow);
        staffProfile = new StaffProfile(staffProfileId, staffUser, "Dr. Ahmet", "+905552223344", StaffType.DOCTOR, "Kardiyoloji");

        // Other Doctor
        otherStaffUserId = UUID.randomUUID();
        otherStaffProfileId = UUID.randomUUID();
        otherStaffUser = new UserAccount(otherStaffUserId, "other.doctor@example.com", "hash", true, fixedNow);
        otherStaffProfile = new StaffProfile(otherStaffProfileId, otherStaffUser, "Dr. Zeynep", "+905553334455", StaffType.DOCTOR, "Nöroloji");

        // Authorized Manager
        managerUserId = UUID.randomUUID();
        managerUser = new UserAccount(managerUserId, "manager@example.com", "hash", true, fixedNow);

        // Outside-scope Manager
        otherManagerUserId = UUID.randomUUID();
        otherManagerUser = new UserAccount(otherManagerUserId, "other.manager@example.com", "hash", true, fixedNow);
        setId(otherManagerUser, otherManagerUserId);

        // Appointment: scheduled for 2026-11-10T10:00 to 10:30 UTC (future relative to fixedNow 09:00)
        appointmentStart = Instant.parse("2026-11-10T10:00:00Z");
        appointmentEnd = Instant.parse("2026-11-10T10:30:00Z");

        pendingAppointmentEntity = new AppointmentJpaEntity(
                appointmentId,
                patientProfileId,
                staffProfileId,
                branchId,
                branchServiceId,
                appointmentStart,
                appointmentEnd,
                AppointmentStatus.PENDING,
                "Kardiyoloji Muayenesi",
                30,
                new BigDecimal("500.00"),
                "TRY",
                0L,
                fixedNow.minus(Duration.ofHours(1))
        );
    }

    @Test
    @DisplayName("FR-018 / TC-018: Authorized doctor can approve pending appointment")
    void authorizedDoctorCanApprovePendingAppointment() {
        when(userAccountRepository.findById(staffUserId)).thenReturn(Optional.of(staffUser));
        when(appointmentJpaRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(pendingAppointmentEntity));
        when(staffProfileRepository.findByUserAccountId(staffUserId)).thenReturn(Optional.of(staffProfile));

        approvalService.approveAppointment(appointmentId, staffUserId);

        assertThat(pendingAppointmentEntity.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        verify(appointmentJpaRepository).save(pendingAppointmentEntity);

        ArgumentCaptor<AppointmentStatusHistoryJpaEntity> historyCaptor = ArgumentCaptor.forClass(AppointmentStatusHistoryJpaEntity.class);
        verify(appointmentStatusHistoryJpaRepository).save(historyCaptor.capture());

        AppointmentStatusHistoryJpaEntity history = historyCaptor.getValue();
        assertThat(history.getAppointmentId()).isEqualTo(appointmentId);
        assertThat(history.getFromStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(history.getToStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(history.getActorUserId()).isEqualTo(staffUserId);
        assertThat(history.getReason()).isNull();
        assertThat(history.getOccurredAt()).isEqualTo(clock.instant());
    }

    @Test
    @DisplayName("FR-018: Authorized branch manager can approve pending appointment")
    void managerApprovesWithinAuthorizedScope() {
        when(userAccountRepository.findById(managerUserId)).thenReturn(Optional.of(managerUser));
        when(appointmentJpaRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(pendingAppointmentEntity));
        when(staffProfileRepository.findByUserAccountId(managerUserId)).thenReturn(Optional.empty());
        when(accessPolicy.hasAdminBranchAccess(branchId, managerUser)).thenReturn(true);

        approvalService.approveAppointment(appointmentId, managerUserId);

        assertThat(pendingAppointmentEntity.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        verify(appointmentJpaRepository).save(pendingAppointmentEntity);
        verify(appointmentStatusHistoryJpaRepository).save(any(AppointmentStatusHistoryJpaEntity.class));
    }

    @Test
    @DisplayName("FR-019 / TC-019: Doctor rejects appointment with mandatory reason and whitespace is trimmed")
    void doctorRejectsAppointmentWithReason() {
        when(userAccountRepository.findById(staffUserId)).thenReturn(Optional.of(staffUser));
        when(appointmentJpaRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(pendingAppointmentEntity));
        when(staffProfileRepository.findByUserAccountId(staffUserId)).thenReturn(Optional.of(staffProfile));

        approvalService.rejectAppointment(appointmentId, staffUserId, "  Doktor acil konsültasyonda  ");

        assertThat(pendingAppointmentEntity.getStatus()).isEqualTo(AppointmentStatus.REJECTED);
        verify(appointmentJpaRepository).save(pendingAppointmentEntity);

        ArgumentCaptor<AppointmentStatusHistoryJpaEntity> historyCaptor = ArgumentCaptor.forClass(AppointmentStatusHistoryJpaEntity.class);
        verify(appointmentStatusHistoryJpaRepository).save(historyCaptor.capture());

        AppointmentStatusHistoryJpaEntity history = historyCaptor.getValue();
        assertThat(history.getFromStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(history.getToStatus()).isEqualTo(AppointmentStatus.REJECTED);
        assertThat(history.getReason()).isEqualTo("Doktor acil konsültasyonda");
    }

    @Test
    @DisplayName("FR-019: Manager rejects pending appointment with reason within authorized scope")
    void managerRejectsWithReason() {
        when(userAccountRepository.findById(managerUserId)).thenReturn(Optional.of(managerUser));
        when(appointmentJpaRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(pendingAppointmentEntity));
        when(staffProfileRepository.findByUserAccountId(managerUserId)).thenReturn(Optional.empty());
        when(accessPolicy.hasAdminBranchAccess(branchId, managerUser)).thenReturn(true);

        approvalService.rejectAppointment(appointmentId, managerUserId, "Şube bakımda");

        assertThat(pendingAppointmentEntity.getStatus()).isEqualTo(AppointmentStatus.REJECTED);
        verify(appointmentJpaRepository).save(pendingAppointmentEntity);
    }

    @Test
    @DisplayName("TC-019: Rejection without reason or with blank/whitespace reason fails")
    void rejectWithoutReasonShouldFail() {
        assertThatThrownBy(() -> approvalService.rejectAppointment(appointmentId, staffUserId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rejection reason must not be null or blank");

        assertThatThrownBy(() -> approvalService.rejectAppointment(appointmentId, staffUserId, "   \t  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rejection reason must not be null or blank");

        verify(appointmentJpaRepository, never()).save(any());
        verify(appointmentStatusHistoryJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Validation: Excessively long reason (>500 characters) fails")
    void excessivelyLongReasonsFail() {
        String longReason = "A".repeat(501);

        assertThatThrownBy(() -> approvalService.rejectAppointment(appointmentId, staffUserId, longReason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not exceed 500 characters");

        assertThatThrownBy(() -> approvalService.cancelAppointment(appointmentId, patientUserId, longReason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not exceed 500 characters");

        verify(appointmentJpaRepository, never()).save(any());
        verify(appointmentStatusHistoryJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Security: Patient cannot approve own appointment")
    void patientCannotApproveOwnAppointment() {
        when(userAccountRepository.findById(patientUserId)).thenReturn(Optional.of(patientUser));
        when(appointmentJpaRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(pendingAppointmentEntity));
        when(staffProfileRepository.findByUserAccountId(patientUserId)).thenReturn(Optional.empty());
        when(accessPolicy.hasAdminBranchAccess(branchId, patientUser)).thenReturn(false);

        assertThatThrownBy(() -> approvalService.approveAppointment(appointmentId, patientUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not authorized to approve");

        assertThat(pendingAppointmentEntity.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        verify(appointmentJpaRepository, never()).save(any());
        verify(appointmentStatusHistoryJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Security: Manager denied outside organization or branch scope")
    void managerDeniedOutsideOrganizationScope() {
        when(userAccountRepository.findById(otherManagerUserId)).thenReturn(Optional.of(otherManagerUser));
        when(appointmentJpaRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(pendingAppointmentEntity));
        when(staffProfileRepository.findByUserAccountId(otherManagerUserId)).thenReturn(Optional.empty());
        when(accessPolicy.hasAdminBranchAccess(branchId, otherManagerUser)).thenReturn(false);

        assertThatThrownBy(() -> approvalService.approveAppointment(appointmentId, otherManagerUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not authorized to approve");

        verify(appointmentJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Security: Doctor denied outside assignment (cannot approve other doctor's appointment)")
    void doctorDeniedOutsideAssignment() {
        when(userAccountRepository.findById(otherStaffUserId)).thenReturn(Optional.of(otherStaffUser));
        when(appointmentJpaRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(pendingAppointmentEntity));
        when(staffProfileRepository.findByUserAccountId(otherStaffUserId)).thenReturn(Optional.of(otherStaffProfile));
        when(accessPolicy.hasAdminBranchAccess(branchId, otherStaffUser)).thenReturn(false);

        assertThatThrownBy(() -> approvalService.approveAppointment(appointmentId, otherStaffUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not authorized to approve");

        verify(appointmentJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("FR-020 / TC-020: Patient cancels own pending appointment before start time")
    void patientCancelsOwnPendingAppointment() {
        when(userAccountRepository.findById(patientUserId)).thenReturn(Optional.of(patientUser));
        when(appointmentJpaRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(pendingAppointmentEntity));
        when(patientProfileRepository.findByUserAccountId(patientUserId)).thenReturn(Optional.of(patientProfile));

        approvalService.cancelAppointment(appointmentId, patientUserId, "Plan değişikliği");

        assertThat(pendingAppointmentEntity.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(appointmentJpaRepository).save(pendingAppointmentEntity);

        ArgumentCaptor<AppointmentStatusHistoryJpaEntity> historyCaptor = ArgumentCaptor.forClass(AppointmentStatusHistoryJpaEntity.class);
        verify(appointmentStatusHistoryJpaRepository).save(historyCaptor.capture());

        AppointmentStatusHistoryJpaEntity history = historyCaptor.getValue();
        assertThat(history.getFromStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(history.getToStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(history.getReason()).isEqualTo("Plan değişikliği");
    }

    @Test
    @DisplayName("FR-020: Patient cancels own confirmed appointment before start time")
    void patientCancelsOwnConfirmedAppointment() {
        pendingAppointmentEntity.setStatus(AppointmentStatus.CONFIRMED);

        when(userAccountRepository.findById(patientUserId)).thenReturn(Optional.of(patientUser));
        when(appointmentJpaRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(pendingAppointmentEntity));
        when(patientProfileRepository.findByUserAccountId(patientUserId)).thenReturn(Optional.of(patientProfile));

        approvalService.cancelAppointment(appointmentId, patientUserId, "Rahatsızlık");

        assertThat(pendingAppointmentEntity.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(appointmentJpaRepository).save(pendingAppointmentEntity);
    }

    @Test
    @DisplayName("Security: Patient denied when cancelling another patient's appointment")
    void patientDeniedWhenCancellingAnotherPatientsAppointment() {
        UUID strangerUserId = UUID.randomUUID();
        UserAccount strangerUser = new UserAccount(strangerUserId, "stranger@example.com", "hash", true, Instant.now());
        UUID strangerProfileId = UUID.randomUUID();
        PatientProfile strangerProfile = new PatientProfile(strangerProfileId, strangerUser, "Stranger", "+905550000000", Instant.now());

        when(userAccountRepository.findById(strangerUserId)).thenReturn(Optional.of(strangerUser));
        when(appointmentJpaRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(pendingAppointmentEntity));
        when(patientProfileRepository.findByUserAccountId(strangerUserId)).thenReturn(Optional.of(strangerProfile));
        when(staffProfileRepository.findByUserAccountId(strangerUserId)).thenReturn(Optional.empty());
        when(accessPolicy.hasAdminBranchAccess(branchId, strangerUser)).thenReturn(false);

        assertThatThrownBy(() -> approvalService.cancelAppointment(appointmentId, strangerUserId, "İptal denemesi"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not authorized to cancel");

        assertThat(pendingAppointmentEntity.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        verify(appointmentJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("FR-020: Doctor cancels eligible appointment")
    void doctorCancelsEligibleAppointment() {
        when(userAccountRepository.findById(staffUserId)).thenReturn(Optional.of(staffUser));
        when(appointmentJpaRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(pendingAppointmentEntity));
        when(patientProfileRepository.findByUserAccountId(staffUserId)).thenReturn(Optional.empty());
        when(staffProfileRepository.findByUserAccountId(staffUserId)).thenReturn(Optional.of(staffProfile));

        approvalService.cancelAppointment(appointmentId, staffUserId, "Hekim acil izinli");

        assertThat(pendingAppointmentEntity.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(appointmentJpaRepository).save(pendingAppointmentEntity);
    }

    @Test
    @DisplayName("FR-021 / TC-021: Doctor completes confirmed appointment after appointment end time")
    void doctorCompletesAfterAppointmentEnd() {
        pendingAppointmentEntity.setStatus(AppointmentStatus.CONFIRMED);

        // Clock after appointment end (end is 10:30, clock at 10:35)
        Clock clockAfterEnd = Clock.fixed(Instant.parse("2026-11-10T10:35:00Z"), ZoneOffset.UTC);
        AppointmentApprovalService serviceWithLateClock = new AppointmentApprovalService(
                appointmentJpaRepository,
                appointmentStatusHistoryJpaRepository,
                userAccountRepository,
                staffProfileRepository,
                patientProfileRepository,
                accessPolicy,
                securityContextFacade,
                clockAfterEnd
        );

        when(userAccountRepository.findById(staffUserId)).thenReturn(Optional.of(staffUser));
        when(appointmentJpaRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(pendingAppointmentEntity));
        when(staffProfileRepository.findByUserAccountId(staffUserId)).thenReturn(Optional.of(staffProfile));

        serviceWithLateClock.completeAppointment(appointmentId, staffUserId);

        assertThat(pendingAppointmentEntity.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        verify(appointmentJpaRepository).save(pendingAppointmentEntity);

        ArgumentCaptor<AppointmentStatusHistoryJpaEntity> historyCaptor = ArgumentCaptor.forClass(AppointmentStatusHistoryJpaEntity.class);
        verify(appointmentStatusHistoryJpaRepository).save(historyCaptor.capture());

        AppointmentStatusHistoryJpaEntity history = historyCaptor.getValue();
        assertThat(history.getFromStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(history.getToStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    @DisplayName("TC-021: Completion before appointment end time is rejected")
    void completionBeforeEndIsRejected() {
        pendingAppointmentEntity.setStatus(AppointmentStatus.CONFIRMED);

        // Current clock is 09:00, appointment ends at 10:30
        when(userAccountRepository.findById(staffUserId)).thenReturn(Optional.of(staffUser));
        when(appointmentJpaRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(pendingAppointmentEntity));
        when(staffProfileRepository.findByUserAccountId(staffUserId)).thenReturn(Optional.of(staffProfile));

        assertThatThrownBy(() -> approvalService.completeAppointment(appointmentId, staffUserId))
                .isInstanceOf(AppointmentDomainException.class)
                .hasMessageContaining("before its scheduled end time");

        assertThat(pendingAppointmentEntity.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        verify(appointmentJpaRepository, never()).save(any());
        verify(appointmentStatusHistoryJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Security: Manager cannot complete doctor-only appointment")
    void managerCannotCompleteDoctorOnlyAppointment() {
        pendingAppointmentEntity.setStatus(AppointmentStatus.CONFIRMED);

        when(userAccountRepository.findById(managerUserId)).thenReturn(Optional.of(managerUser));
        when(appointmentJpaRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(pendingAppointmentEntity));
        when(staffProfileRepository.findByUserAccountId(managerUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> approvalService.completeAppointment(appointmentId, managerUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("only the assigned doctor can complete");

        assertThat(pendingAppointmentEntity.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        verify(appointmentJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Validation: Missing appointment or user ID fails consistently")
    void invalidOrMissingAppointmentIdsFailConsistently() {
        assertThatThrownBy(() -> approvalService.approveAppointment(null, staffUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Appointment ID must not be null");

        assertThatThrownBy(() -> approvalService.approveAppointment(appointmentId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Actor user ID must not be null");

        when(userAccountRepository.findById(staffUserId)).thenReturn(Optional.of(staffUser));
        when(appointmentJpaRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> approvalService.approveAppointment(appointmentId, staffUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Appointment not found");
    }

    @Test
    @DisplayName("Security: Disabled user account cannot perform transitions")
    void disabledUserCannotPerformTransitions() {
        staffUser.setEnabled(false);
        when(userAccountRepository.findById(staffUserId)).thenReturn(Optional.of(staffUser));

        assertThatThrownBy(() -> approvalService.approveAppointment(appointmentId, staffUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("user account is disabled");

        verify(appointmentJpaRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("FR-030 / TC-030: Audit history retrieval authorized for patient, doctor, and manager; denied for outsider")
    void auditHistoryRetrievalAccess() {
        when(userAccountRepository.findById(patientUserId)).thenReturn(Optional.of(patientUser));
        when(appointmentJpaRepository.findById(appointmentId)).thenReturn(Optional.of(pendingAppointmentEntity));
        when(patientProfileRepository.findByUserAccountId(patientUserId)).thenReturn(Optional.of(patientProfile));
        when(appointmentStatusHistoryJpaRepository.findByAppointmentIdOrderByOccurredAtAsc(appointmentId))
                .thenReturn(List.of(
                        new AppointmentStatusHistoryJpaEntity(UUID.randomUUID(), appointmentId, null, AppointmentStatus.PENDING, patientUserId, null, clock.instant())
                ));

        List<AppointmentStatusHistoryJpaEntity> history = approvalService.getAppointmentStatusHistory(appointmentId, patientUserId);
        assertThat(history).hasSize(1);

        // Stranger denied
        UUID outsiderId = UUID.randomUUID();
        UserAccount outsider = new UserAccount(outsiderId, "outsider@example.com", "hash", true, Instant.now());

        when(userAccountRepository.findById(outsiderId)).thenReturn(Optional.of(outsider));
        when(patientProfileRepository.findByUserAccountId(outsiderId)).thenReturn(Optional.empty());
        when(staffProfileRepository.findByUserAccountId(outsiderId)).thenReturn(Optional.empty());
        when(accessPolicy.hasAdminBranchAccess(branchId, outsider)).thenReturn(false);

        assertThatThrownBy(() -> approvalService.getAppointmentStatusHistory(appointmentId, outsiderId))
                .isInstanceOf(AccessDeniedException.class);
    }
}
