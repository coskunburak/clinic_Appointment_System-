package com.example.clinic.booking.application;

import com.example.clinic.booking.domain.Appointment;
import com.example.clinic.booking.domain.AppointmentStatus;
import com.example.clinic.booking.infrastructure.persistence.AppointmentJpaEntity;
import com.example.clinic.booking.infrastructure.persistence.AppointmentJpaRepository;
import com.example.clinic.booking.infrastructure.persistence.AppointmentStatusHistoryJpaEntity;
import com.example.clinic.booking.infrastructure.persistence.AppointmentStatusHistoryJpaRepository;
import com.example.clinic.common.security.SecurityContextFacade;
import com.example.clinic.identity.application.AccessPolicy;
import com.example.clinic.identity.domain.PatientProfile;
import com.example.clinic.identity.domain.StaffProfile;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.PatientProfileRepository;
import com.example.clinic.identity.infrastructure.StaffProfileRepository;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Production-level transactional application service governing the appointment lifecycle and audit trail.
 * Implements Sprint S07 requirements (FR-018, FR-019, FR-020, FR-021, FR-030).
 * <p>
 * Key Invariants:
 * <ul>
 *   <li>Row-level pessimistic locking ({@code findByIdForUpdate}) guarantees race-safe state transitions.</li>
 *   <li>Authoritative multi-tenant authorization via {@link AccessPolicy} and identity profiles.</li>
 *   <li>Pure domain aggregate enforces state machine invariants (PENDING &rarr; CONFIRMED/REJECTED/CANCELLED, etc.).</li>
 *   <li>Atomic state and append-only status history persistence in the same database transaction.</li>
 *   <li>Rejection and cancellation release staff availability without physical appointment deletion (ADR-003).</li>
 * </ul>
 */
@Service
public class AppointmentApprovalService {

    private final AppointmentJpaRepository appointmentJpaRepository;
    private final AppointmentStatusHistoryJpaRepository appointmentStatusHistoryJpaRepository;
    private final UserAccountRepository userAccountRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final AccessPolicy accessPolicy;
    private final SecurityContextFacade securityContextFacade;
    private final Clock clock;

    public AppointmentApprovalService(AppointmentJpaRepository appointmentJpaRepository,
                                      AppointmentStatusHistoryJpaRepository appointmentStatusHistoryJpaRepository,
                                      UserAccountRepository userAccountRepository,
                                      StaffProfileRepository staffProfileRepository,
                                      PatientProfileRepository patientProfileRepository,
                                      AccessPolicy accessPolicy,
                                      SecurityContextFacade securityContextFacade,
                                      Clock clock) {
        this.appointmentJpaRepository = Objects.requireNonNull(appointmentJpaRepository, "appointmentJpaRepository must not be null");
        this.appointmentStatusHistoryJpaRepository = Objects.requireNonNull(appointmentStatusHistoryJpaRepository, "appointmentStatusHistoryJpaRepository must not be null");
        this.userAccountRepository = Objects.requireNonNull(userAccountRepository, "userAccountRepository must not be null");
        this.staffProfileRepository = Objects.requireNonNull(staffProfileRepository, "staffProfileRepository must not be null");
        this.patientProfileRepository = Objects.requireNonNull(patientProfileRepository, "patientProfileRepository must not be null");
        this.accessPolicy = Objects.requireNonNull(accessPolicy, "accessPolicy must not be null");
        this.securityContextFacade = Objects.requireNonNull(securityContextFacade, "securityContextFacade must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Approves a PENDING appointment, transitioning it to CONFIRMED (FR-018, TC-018).
     * Authorized actors: assigned doctor or authorized branch/organization manager.
     *
     * @param appointmentId the target appointment ID
     * @param actorUserId   the authoritative user ID of the actor
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void approveAppointment(UUID appointmentId, UUID actorUserId) {
        validateId(appointmentId, "Appointment ID");
        validateId(actorUserId, "Actor user ID");

        UserAccount actorUser = resolveUserOrThrow(actorUserId);
        AppointmentJpaEntity entity = loadAppointmentForUpdate(appointmentId);
        Appointment appointment = entity.toDomain();

        authorizeDoctorOrManager(appointment, actorUser, "approve");

        AppointmentStatus fromStatus = appointment.getStatus();
        appointment.approve(actorUserId, clock);
        AppointmentStatus toStatus = appointment.getStatus();

        entity.setStatus(toStatus);
        appointmentJpaRepository.save(entity);

        AppointmentStatusHistoryJpaEntity history = new AppointmentStatusHistoryJpaEntity(
                UUID.randomUUID(),
                appointment.getId(),
                fromStatus,
                toStatus,
                actorUserId,
                null,
                clock.instant()
        );
        appointmentStatusHistoryJpaRepository.save(history);
    }

    /**
     * Rejects a PENDING appointment with a mandatory non-blank reason (FR-019, TC-019).
     * Releases staff availability slot (ADR-003).
     * Authorized actors: assigned doctor or authorized branch/organization manager.
     *
     * @param appointmentId the target appointment ID
     * @param actorUserId   the authoritative user ID of the actor
     * @param reason        mandatory rejection reason (1..500 characters)
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void rejectAppointment(UUID appointmentId, UUID actorUserId, String reason) {
        validateId(appointmentId, "Appointment ID");
        validateId(actorUserId, "Actor user ID");
        String trimmedReason = validateReason(reason, "Rejection");

        UserAccount actorUser = resolveUserOrThrow(actorUserId);
        AppointmentJpaEntity entity = loadAppointmentForUpdate(appointmentId);
        Appointment appointment = entity.toDomain();

        authorizeDoctorOrManager(appointment, actorUser, "reject");

        AppointmentStatus fromStatus = appointment.getStatus();
        appointment.reject(actorUserId, trimmedReason, clock);
        AppointmentStatus toStatus = appointment.getStatus();

        entity.setStatus(toStatus);
        appointmentJpaRepository.save(entity);

        AppointmentStatusHistoryJpaEntity history = new AppointmentStatusHistoryJpaEntity(
                UUID.randomUUID(),
                appointment.getId(),
                fromStatus,
                toStatus,
                actorUserId,
                trimmedReason,
                clock.instant()
        );
        appointmentStatusHistoryJpaRepository.save(history);
    }

    /**
     * Cancels an appointment from PENDING or CONFIRMED status before start time (FR-020, TC-020).
     * Releases staff availability slot (ADR-003).
     * Authorized actors: owning patient, assigned doctor, or authorized manager.
     *
     * @param appointmentId the target appointment ID
     * @param actorUserId   the authoritative user ID of the actor
     * @param reason        mandatory cancellation reason (1..500 characters)
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancelAppointment(UUID appointmentId, UUID actorUserId, String reason) {
        validateId(appointmentId, "Appointment ID");
        validateId(actorUserId, "Actor user ID");
        String trimmedReason = validateReason(reason, "Cancellation");

        UserAccount actorUser = resolveUserOrThrow(actorUserId);
        AppointmentJpaEntity entity = loadAppointmentForUpdate(appointmentId);
        Appointment appointment = entity.toDomain();

        authorizeDoctorOrManagerOrPatient(appointment, actorUser);

        AppointmentStatus fromStatus = appointment.getStatus();
        appointment.cancel(actorUserId, trimmedReason, clock);
        AppointmentStatus toStatus = appointment.getStatus();

        entity.setStatus(toStatus);
        appointmentJpaRepository.save(entity);

        AppointmentStatusHistoryJpaEntity history = new AppointmentStatusHistoryJpaEntity(
                UUID.randomUUID(),
                appointment.getId(),
                fromStatus,
                toStatus,
                actorUserId,
                trimmedReason,
                clock.instant()
        );
        appointmentStatusHistoryJpaRepository.save(history);
    }

    /**
     * Completes a CONFIRMED appointment once its scheduled end time has been reached (FR-021, TC-021).
     * Retains slot reservation in history.
     * Authorized actors: assigned doctor ONLY. Managers and other staff are denied.
     *
     * @param appointmentId the target appointment ID
     * @param actorUserId   the authoritative user ID of the assigned doctor
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void completeAppointment(UUID appointmentId, UUID actorUserId) {
        validateId(appointmentId, "Appointment ID");
        validateId(actorUserId, "Actor user ID");

        UserAccount actorUser = resolveUserOrThrow(actorUserId);
        AppointmentJpaEntity entity = loadAppointmentForUpdate(appointmentId);
        Appointment appointment = entity.toDomain();

        authorizeAssignedDoctorOnly(appointment, actorUser);

        AppointmentStatus fromStatus = appointment.getStatus();
        appointment.complete(actorUserId, clock);
        AppointmentStatus toStatus = appointment.getStatus();

        entity.setStatus(toStatus);
        appointmentJpaRepository.save(entity);

        AppointmentStatusHistoryJpaEntity history = new AppointmentStatusHistoryJpaEntity(
                UUID.randomUUID(),
                appointment.getId(),
                fromStatus,
                toStatus,
                actorUserId,
                null,
                clock.instant()
        );
        appointmentStatusHistoryJpaRepository.save(history);
    }

    /**
     * Retrieves the complete append-only audit trail for an appointment (FR-030, TC-030).
     * Deterministically sorted by {@code occurred_at ASC, id ASC}.
     *
     * @param appointmentId target appointment ID
     * @param actorUserId   authorized actor (patient owner, assigned doctor, or branch manager)
     * @return chronological list of status transitions
     */
    @Transactional(readOnly = true)
    public List<AppointmentStatusHistoryJpaEntity> getAppointmentStatusHistory(UUID appointmentId, UUID actorUserId) {
        validateId(appointmentId, "Appointment ID");
        validateId(actorUserId, "Actor user ID");

        UserAccount actorUser = resolveUserOrThrow(actorUserId);
        AppointmentJpaEntity entity = appointmentJpaRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));
        Appointment appointment = entity.toDomain();

        authorizeDoctorOrManagerOrPatient(appointment, actorUser);

        return appointmentStatusHistoryJpaRepository.findByAppointmentIdOrderByOccurredAtAsc(appointmentId);
    }

    // Convenience methods leveraging SecurityContextFacade for future S08 controllers

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void approveAppointment(UUID appointmentId) {
        approveAppointment(appointmentId, securityContextFacade.getRequiredCurrentUser().getId());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void rejectAppointment(UUID appointmentId, String reason) {
        rejectAppointment(appointmentId, securityContextFacade.getRequiredCurrentUser().getId(), reason);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancelAppointment(UUID appointmentId, String reason) {
        cancelAppointment(appointmentId, securityContextFacade.getRequiredCurrentUser().getId(), reason);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void completeAppointment(UUID appointmentId) {
        completeAppointment(appointmentId, securityContextFacade.getRequiredCurrentUser().getId());
    }

    @Transactional(readOnly = true)
    public List<AppointmentStatusHistoryJpaEntity> getAppointmentStatusHistory(UUID appointmentId) {
        return getAppointmentStatusHistory(appointmentId, securityContextFacade.getRequiredCurrentUser().getId());
    }

    // Helper validation & authorization methods

    private void validateId(UUID id, String fieldName) {
        if (id == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }

    private String validateReason(String reason, String operation) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(operation + " reason must not be null or blank");
        }
        String trimmed = reason.strip();
        if (trimmed.length() > 500) {
            throw new IllegalArgumentException(operation + " reason must not exceed 500 characters");
        }
        return trimmed;
    }

    private UserAccount resolveUserOrThrow(UUID actorUserId) {
        UserAccount user = userAccountRepository.findById(actorUserId)
                .orElseThrow(() -> new AccessDeniedException("Access denied: user account not found: " + actorUserId));
        if (!user.isEnabled()) {
            throw new AccessDeniedException("Access denied: user account is disabled: " + actorUserId);
        }
        return user;
    }

    private AppointmentJpaEntity loadAppointmentForUpdate(UUID appointmentId) {
        return appointmentJpaRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));
    }

    private void authorizeDoctorOrManager(Appointment appointment, UserAccount actorUser, String action) {
        // 1. Check if actor is the assigned staff member
        Optional<StaffProfile> staffOpt = staffProfileRepository.findByUserAccountId(actorUser.getId());
        if (staffOpt.isPresent() && staffOpt.get().isActive() && staffOpt.get().getId().equals(appointment.getStaffId())) {
            return;
        }

        // 2. Check if actor is an authorized manager with branch/organization scope
        if (accessPolicy.hasAdminBranchAccess(appointment.getBranchId(), actorUser)) {
            return;
        }

        throw new AccessDeniedException("Access denied: user " + actorUser.getId() +
                " is not authorized to " + action + " appointment " + appointment.getId());
    }

    private void authorizeDoctorOrManagerOrPatient(Appointment appointment, UserAccount actorUser) {
        // 1. Check if actor is the owning patient
        Optional<PatientProfile> patientOpt = patientProfileRepository.findByUserAccountId(actorUser.getId());
        if (patientOpt.isPresent() && patientOpt.get().getId().equals(appointment.getPatientId())) {
            return;
        }

        // 2. Check if actor is the assigned staff member
        Optional<StaffProfile> staffOpt = staffProfileRepository.findByUserAccountId(actorUser.getId());
        if (staffOpt.isPresent() && staffOpt.get().isActive() && staffOpt.get().getId().equals(appointment.getStaffId())) {
            return;
        }

        // 3. Check if actor is an authorized manager
        if (accessPolicy.hasAdminBranchAccess(appointment.getBranchId(), actorUser)) {
            return;
        }

        throw new AccessDeniedException("Access denied: user " + actorUser.getId() +
                " is not authorized to cancel appointment " + appointment.getId());
    }

    private void authorizeAssignedDoctorOnly(Appointment appointment, UserAccount actorUser) {
        Optional<StaffProfile> staffOpt = staffProfileRepository.findByUserAccountId(actorUser.getId());
        if (staffOpt.isEmpty() || !staffOpt.get().isActive() || !staffOpt.get().getId().equals(appointment.getStaffId())) {
            throw new AccessDeniedException("Access denied: only the assigned doctor can complete appointment " +
                    appointment.getId() + " (actor=" + actorUser.getId() + ")");
        }
    }
}
