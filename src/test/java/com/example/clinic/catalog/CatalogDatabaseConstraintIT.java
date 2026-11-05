package com.example.clinic.catalog;

import com.example.clinic.AbstractIntegrationTest;
import com.example.clinic.catalog.domain.BranchService;
import com.example.clinic.catalog.domain.ServiceDefinition;
import com.example.clinic.catalog.infrastructure.BranchServiceRepository;
import com.example.clinic.catalog.infrastructure.ServiceDefinitionRepository;
import com.example.clinic.common.domain.Money;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.Organization;
import com.example.clinic.organization.infrastructure.BranchRepository;
import com.example.clinic.organization.infrastructure.OrganizationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class CatalogDatabaseConstraintIT extends AbstractIntegrationTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private ServiceDefinitionRepository serviceDefinitionRepository;

    @Autowired
    private BranchServiceRepository branchServiceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("DB Constraint: should reject duplicate service definition name in the same organization via uq_service_definition_org_name")
    void shouldRejectDuplicateServiceNameInSameOrg() {
        Organization org = organizationRepository.saveAndFlush(new Organization("Org A"));

        ServiceDefinition def1 = new ServiceDefinition(org, "Genel Muayene", "Açıklama 1");
        serviceDefinitionRepository.saveAndFlush(def1);

        ServiceDefinition def2 = new ServiceDefinition(org, "Genel Muayene", "Açıklama 2");

        assertThatThrownBy(() -> serviceDefinitionRepository.saveAndFlush(def2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("DB Constraint: should allow identical service definition name in different organizations")
    void shouldAllowSameServiceNameInDifferentOrgs() {
        Organization orgA = organizationRepository.saveAndFlush(new Organization("Org A"));
        Organization orgB = organizationRepository.saveAndFlush(new Organization("Org B"));

        ServiceDefinition defA = new ServiceDefinition(orgA, "Genel Muayene", "Açıklama A");
        ServiceDefinition defB = new ServiceDefinition(orgB, "Genel Muayene", "Açıklama B");

        serviceDefinitionRepository.saveAndFlush(defA);
        ServiceDefinition savedB = serviceDefinitionRepository.saveAndFlush(defB);

        assertThat(savedB.getId()).isNotNull();
    }

    @Test
    @DisplayName("DB Constraint: should reject duplicate branch-service assignment via uq_branch_service_unique")
    void shouldRejectDuplicateBranchServiceAssignment() {
        Organization org = organizationRepository.saveAndFlush(new Organization("Org Unique"));
        Branch branch = branchRepository.saveAndFlush(new Branch(org, "Şube", "Europe/Istanbul", "Adres"));
        ServiceDefinition def = serviceDefinitionRepository.saveAndFlush(new ServiceDefinition(org, "Laboratuvar", null));

        Money price1 = Money.of(new BigDecimal("500.00"), Currency.getInstance("TRY"));
        BranchService bs1 = new BranchService(branch, def, 30, price1);
        branchServiceRepository.saveAndFlush(bs1);

        Money price2 = Money.of(new BigDecimal("750.00"), Currency.getInstance("TRY"));
        BranchService bs2 = new BranchService(branch, def, 45, price2);

        assertThatThrownBy(() -> branchServiceRepository.saveAndFlush(bs2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("DB Constraint: should reject duration below 1 via chk_branch_service_duration at database level")
    void shouldRejectInvalidDurationCheckAtDatabaseLevel() {
        Organization org = organizationRepository.saveAndFlush(new Organization("Org Duration Check"));
        Branch branch = branchRepository.saveAndFlush(new Branch(org, "Şube Duration", "Europe/Istanbul", "Adres"));
        ServiceDefinition def = serviceDefinitionRepository.saveAndFlush(new ServiceDefinition(org, "Radyoloji", null));

        // Directly insert invalid duration 0 via SQL to bypass Java entity validation
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO branch_service (id, organization_id, branch_id, service_definition_id, duration_minutes, fee_amount, currency, active, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                UUID.randomUUID(), org.getId(), branch.getId(), def.getId(), 0, new BigDecimal("1000.00"), "TRY", true
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("DB Constraint: should reject duration above 480 via chk_branch_service_duration at database level")
    void shouldRejectDurationAboveMaxCheckAtDatabaseLevel() {
        Organization org = organizationRepository.saveAndFlush(new Organization("Org Duration Max"));
        Branch branch = branchRepository.saveAndFlush(new Branch(org, "Şube Max", "Europe/Istanbul", "Adres"));
        ServiceDefinition def = serviceDefinitionRepository.saveAndFlush(new ServiceDefinition(org, "Ameliyat", null));

        // Directly insert invalid duration 481 via SQL
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO branch_service (id, organization_id, branch_id, service_definition_id, duration_minutes, fee_amount, currency, active, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                UUID.randomUUID(), org.getId(), branch.getId(), def.getId(), 481, new BigDecimal("5000.00"), "TRY", true
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("DB Constraint: should reject negative fee via chk_branch_service_fee at database level")
    void shouldRejectNegativeFeeCheckAtDatabaseLevel() {
        Organization org = organizationRepository.saveAndFlush(new Organization("Org Fee Check"));
        Branch branch = branchRepository.saveAndFlush(new Branch(org, "Şube Fee", "Europe/Istanbul", "Adres"));
        ServiceDefinition def = serviceDefinitionRepository.saveAndFlush(new ServiceDefinition(org, "Fizik Tedavi", null));

        // Directly insert negative fee via SQL to bypass Money validation
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO branch_service (id, organization_id, branch_id, service_definition_id, duration_minutes, fee_amount, currency, active, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                UUID.randomUUID(), org.getId(), branch.getId(), def.getId(), 30, new BigDecimal("-10.00"), "TRY", true
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("DB Constraint: should reject cross-tenant mismatch via composite foreign keys at database level")
    void shouldRejectCrossTenantMismatchAtDatabaseLevel() {
        Organization orgA = organizationRepository.saveAndFlush(new Organization("Org A Tenant"));
        Organization orgB = organizationRepository.saveAndFlush(new Organization("Org B Tenant"));

        Branch branchOfOrgA = branchRepository.saveAndFlush(
                new Branch(orgA, "Branch A", "Europe/Istanbul", "Adres A")
        );
        ServiceDefinition defOfOrgB = serviceDefinitionRepository.saveAndFlush(
                new ServiceDefinition(orgB, "Service B", "Tanım B")
        );

        // Attempting to insert a branch_service associating branchOfOrgA with defOfOrgB:
        // If organization_id is set to orgA.getId(), fk_branch_service_def_org (defOfOrgB, orgA) fails!
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO branch_service (id, organization_id, branch_id, service_definition_id, duration_minutes, fee_amount, currency, active, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                UUID.randomUUID(), orgA.getId(), branchOfOrgA.getId(), defOfOrgB.getId(), 30, new BigDecimal("1000.00"), "TRY", true
        )).isInstanceOf(DataIntegrityViolationException.class);

        // If organization_id is set to orgB.getId(), fk_branch_service_branch_org (branchOfOrgA, orgB) fails!
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO branch_service (id, organization_id, branch_id, service_definition_id, duration_minutes, fee_amount, currency, active, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                UUID.randomUUID(), orgB.getId(), branchOfOrgA.getId(), defOfOrgB.getId(), 30, new BigDecimal("1000.00"), "TRY", true
        )).isInstanceOf(DataIntegrityViolationException.class);
    }
}
