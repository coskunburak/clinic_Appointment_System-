package com.example.clinic.booking.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AppointmentStatusHistoryJpaEntity}.
 * Guarantees deterministic chronological retrieval using primary key (id) as secondary ordering key.
 */
@Repository
public interface AppointmentStatusHistoryJpaRepository extends JpaRepository<AppointmentStatusHistoryJpaEntity, UUID> {

    /**
     * Retrieves all status history records for an appointment, ordered chronologically by occurred_at ASC
     * and deterministically by id ASC when timestamps match.
     */
    @Query("SELECT h FROM AppointmentStatusHistoryJpaEntity h WHERE h.appointmentId = :appointmentId ORDER BY h.occurredAt ASC, h.id ASC")
    List<AppointmentStatusHistoryJpaEntity> findByAppointmentIdOrderByOccurredAtAsc(@Param("appointmentId") UUID appointmentId);

    List<AppointmentStatusHistoryJpaEntity> findByAppointmentIdOrderByOccurredAtAscIdAsc(UUID appointmentId);

    @Query("SELECT h.id FROM AppointmentStatusHistoryJpaEntity h WHERE h.appointmentId IN :appointmentIds")
    List<UUID> findIdsByAppointmentIdIn(@Param("appointmentIds") Collection<UUID> appointmentIds);

    @Query("SELECT h.id FROM AppointmentStatusHistoryJpaEntity h WHERE h.actorUserId IN :actorUserIds")
    List<UUID> findIdsByActorUserIdIn(@Param("actorUserIds") Collection<UUID> actorUserIds);
}
