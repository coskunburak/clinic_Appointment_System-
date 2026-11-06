package com.example.clinic.identity.infrastructure;

import com.example.clinic.identity.domain.Membership;
import com.example.clinic.identity.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, UUID> {
    List<Membership> findByUserAccountId(UUID userAccountId);
    List<Membership> findByOrganizationId(UUID organizationId);
    List<Membership> findByUserAccountIdAndRole(UUID userAccountId, Role role);
    List<Membership> findByUserAccountIdAndOrganizationId(UUID userAccountId, UUID organizationId);
    List<Membership> findByUserAccountIdAndBranchId(UUID userAccountId, UUID branchId);
}
