package com.example.clinic.booking.infrastructure.persistence;

import com.example.clinic.booking.domain.AppointmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AppointmentJpaEntity}.
 * Supplies authoritative conflict queries using half-open {@code [start, end)} interval intersection,
 * and pessimistic write locking for lifecycle state transitions.
 */
@Repository
public interface AppointmentJpaRepository extends JpaRepository<AppointmentJpaEntity, UUID> {

    /**
     * Acquires a row-level pessimistic write lock (SELECT ... FOR UPDATE) on the appointment row.
     * Guarantees deterministic serialization for concurrent lifecycle operations.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AppointmentJpaEntity a WHERE a.id = :id")
    Optional<AppointmentJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Finds appointments for a given staff member that overlap with the target interval {@code [startAtUtc, endAtUtc)}
     * and match any of the specified blocking statuses.
     */
    @Query("SELECT a FROM AppointmentJpaEntity a WHERE a.staffId = :staffId " +
            "AND a.status IN :blockingStatuses " +
            "AND a.startAtUtc < :endAtUtc AND a.endAtUtc > :startAtUtc")
    List<AppointmentJpaEntity> findOverlappingAppointmentsForStaff(
            @Param("staffId") UUID staffId,
            @Param("startAtUtc") Instant startAtUtc,
            @Param("endAtUtc") Instant endAtUtc,
            @Param("blockingStatuses") Collection<AppointmentStatus> blockingStatuses
    );

    /**
     * Finds appointments for a given patient that overlap with the target interval {@code [startAtUtc, endAtUtc)}
     * and match any of the specified blocking statuses (FR-029).
     */
    @Query("SELECT a FROM AppointmentJpaEntity a WHERE a.patientId = :patientId " +
            "AND a.status IN :blockingStatuses " +
            "AND a.startAtUtc < :endAtUtc AND a.endAtUtc > :startAtUtc")
    List<AppointmentJpaEntity> findOverlappingAppointmentsForPatient(
            @Param("patientId") UUID patientId,
            @Param("startAtUtc") Instant startAtUtc,
            @Param("endAtUtc") Instant endAtUtc,
            @Param("blockingStatuses") Collection<AppointmentStatus> blockingStatuses
    );

    /**
     * Queries occupied intervals for a staff member within a given time window (e.g. one local calendar day in UTC).
     */
    @Query("SELECT a FROM AppointmentJpaEntity a WHERE a.staffId = :staffId " +
            "AND a.status IN :blockingStatuses " +
            "AND a.startAtUtc < :windowEnd AND a.endAtUtc > :windowStart " +
            "ORDER BY a.startAtUtc ASC")
    List<AppointmentJpaEntity> findOccupiedAppointmentsForStaff(
            @Param("staffId") UUID staffId,
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd,
            @Param("blockingStatuses") Collection<AppointmentStatus> blockingStatuses
    );

    List<AppointmentJpaEntity> findByPatientIdOrderByStartAtUtcDesc(UUID patientId);

    Page<AppointmentJpaEntity> findByPatientIdOrderByStartAtUtcDesc(UUID patientId, Pageable pageable);

    List<AppointmentJpaEntity> findByStaffIdOrderByStartAtUtcAsc(UUID staffId);

    List<AppointmentJpaEntity> findByStaffIdAndStatusOrderByStartAtUtcAsc(UUID staffId, AppointmentStatus status);

    @Query("SELECT a FROM AppointmentJpaEntity a WHERE a.staffId = :staffId " +
            "AND a.startAtUtc >= :windowStart AND a.startAtUtc < :windowEnd " +
            "ORDER BY a.startAtUtc ASC")
    List<AppointmentJpaEntity> findByStaffIdAndWindow(
            @Param("staffId") UUID staffId,
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd
    );

    List<AppointmentJpaEntity> findByBranchId(UUID branchId);
}
