package com.example.clinic.organization.infrastructure;

import com.example.clinic.organization.domain.BranchOpeningHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BranchOpeningHoursRepository extends JpaRepository<BranchOpeningHours, UUID> {
    List<BranchOpeningHours> findByBranchId(UUID branchId);
}
