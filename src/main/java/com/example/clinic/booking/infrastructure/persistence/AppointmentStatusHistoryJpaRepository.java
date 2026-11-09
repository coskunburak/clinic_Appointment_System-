package com.example.clinic.booking.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AppointmentStatusHistoryJpaEntity}.
 */
@Repository
public interface AppointmentStatusHistoryJpaRepository extends JpaRepository<AppointmentStatusHistoryJpaEntity, UUID> {

    List<AppointmentStatusHistoryJpaEntity> findByAppointmentIdOrderByOccurredAtAsc(UUID appointmentId);
}
