package com.example.clinic.organization.infrastructure;

import com.example.clinic.organization.domain.BranchOpeningHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BranchOpeningHoursRepository extends JpaRepository<BranchOpeningHours, UUID> {

    List<BranchOpeningHours> findByBranchId(UUID branchId);

    List<BranchOpeningHours> findByBranchIdAndWeekday(UUID branchId, short weekday);

    List<BranchOpeningHours> findByBranchIdOrderByWeekdayAscStartTimeAsc(UUID branchId);

    @Modifying
    @Query("DELETE FROM BranchOpeningHours b WHERE b.branch.id = :branchId")
    void deleteByBranchId(@Param("branchId") UUID branchId);
}
