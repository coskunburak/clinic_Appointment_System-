package com.example.clinic.identity.infrastructure;

import com.example.clinic.identity.domain.StaffProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffProfileRepository extends JpaRepository<StaffProfile, UUID> {
    Optional<StaffProfile> findByUserAccountId(UUID userAccountId);

    @Query("""
        SELECT DISTINCT s FROM StaffProfile s
        JOIN FETCH s.userAccount ua
        WHERE EXISTS (
            SELECT 1 FROM StaffAssignment a
            WHERE a.staff = s AND a.branch.id IN :branchIds
        ) OR EXISTS (
            SELECT 1 FROM Membership m
            WHERE m.userAccount = ua
              AND m.role = com.example.clinic.identity.domain.Role.ROLE_STAFF
              AND m.branch.id IN :branchIds
        )
        ORDER BY s.fullName ASC
    """)
    List<StaffProfile> findAuthorizedStaffByBranchIds(@Param("branchIds") Collection<UUID> branchIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StaffProfile s WHERE s.id = :id")
    Optional<StaffProfile> findByIdForUpdate(@Param("id") UUID id);
}
