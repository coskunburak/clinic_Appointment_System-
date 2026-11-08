package com.example.clinic.staff.infrastructure;

import com.example.clinic.staff.domain.WeeklyAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WeeklyAvailabilityRepository extends JpaRepository<WeeklyAvailability, UUID> {

    List<WeeklyAvailability> findByAssignmentId(UUID assignmentId);

    List<WeeklyAvailability> findByAssignmentIdAndWeekday(UUID assignmentId, short weekday);

    List<WeeklyAvailability> findByAssignmentStaffId(UUID staffId);

    List<WeeklyAvailability> findByAssignmentStaffIdAndWeekday(UUID staffId, short weekday);

    void deleteByAssignmentId(UUID assignmentId);
}
