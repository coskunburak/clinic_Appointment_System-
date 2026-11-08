package com.example.clinic.staff.infrastructure;

import com.example.clinic.staff.domain.AvailabilityException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface AvailabilityExceptionRepository extends JpaRepository<AvailabilityException, UUID> {

    List<AvailabilityException> findByAssignmentId(UUID assignmentId);

    List<AvailabilityException> findByAssignmentIdAndLocalDate(UUID assignmentId, LocalDate localDate);

    List<AvailabilityException> findByAssignmentStaffIdAndLocalDate(UUID staffId, LocalDate localDate);

    List<AvailabilityException> findByAssignmentIdAndLocalDateBetween(UUID assignmentId, LocalDate start, LocalDate end);

    void deleteByAssignmentId(UUID assignmentId);
}
