package com.example.clinic.booking;

import com.example.clinic.AbstractIntegrationTest;
import com.example.clinic.catalog.domain.BranchService;
import com.example.clinic.catalog.domain.ServiceDefinition;
import com.example.clinic.catalog.infrastructure.BranchServiceRepository;
import com.example.clinic.catalog.infrastructure.ServiceDefinitionRepository;
import com.example.clinic.common.domain.Money;
import com.example.clinic.identity.domain.PatientProfile;
import com.example.clinic.identity.domain.StaffProfile;
import com.example.clinic.identity.domain.StaffType;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.PatientProfileRepository;
import com.example.clinic.identity.infrastructure.StaffProfileRepository;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.Organization;
import com.example.clinic.organization.infrastructure.BranchRepository;
import com.example.clinic.organization.infrastructure.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests directly exercising PostgreSQL database constraints defined in {@code V4__create_appointments.sql}.
 * Validates check constraints, foreign keys, and the tenant integrity composite foreign key.
 */
@Transactional
class AppointmentDatabaseConstraintIT extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private ServiceDefinitionRepository serviceDefinitionRepository;

    @Autowired
    private BranchServiceRepository branchServiceRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private StaffProfileRepository staffProfileRepository;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    private JdbcTemplate jdbcTemplate;
    private UUID patientId;
    private UUID staffId;
    private UUID branchAId;
    private UUID branchBId;
    private UUID branchServiceAId;
    private UUID branchServiceBId;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);

        Organization org = organizationRepository.saveAndFlush(new Organization("DB Constraint Clinic"));
        Branch branchA = branchRepository.saveAndFlush(new Branch(org, "Branch A", "Europe/Istanbul", "Street 1"));
        Branch branchB = branchRepository.saveAndFlush(new Branch(org, "Branch B", "Europe/Istanbul", "Street 2"));
        branchAId = branchA.getId();
        branchBId = branchB.getId();

        ServiceDefinition def = serviceDefinitionRepository.saveAndFlush(new ServiceDefinition(org, "Exam", "General"));
        BranchService bsA = branchServiceRepository.saveAndFlush(
                new BranchService(org, branchA, def, 30, Money.of(new BigDecimal("400.00"), Currency.getInstance("TRY")))
        );
        BranchService bsB = branchServiceRepository.saveAndFlush(
                new BranchService(org, branchB, def, 30, Money.of(new BigDecimal("450.00"), Currency.getInstance("TRY")))
        );
        branchServiceAId = bsA.getId();
        branchServiceBId = bsB.getId();

        UserAccount staffUser = userAccountRepository.saveAndFlush(new UserAccount("doc.constraint." + UUID.randomUUID() + "@clinic.test", "hash123"));
        StaffProfile staff = staffProfileRepository.saveAndFlush(new StaffProfile(staffUser, "Dr. Constraint", "+905550001122", StaffType.DOCTOR, "Internal"));
        staffId = staff.getId();

        UserAccount patientUser = userAccountRepository.saveAndFlush(new UserAccount("pat.constraint." + UUID.randomUUID() + "@clinic.test", "hash123"));
        PatientProfile patient = patientProfileRepository.saveAndFlush(new PatientProfile(patientUser, "Patient Constraint", "+905550003344"));
        patientId = patient.getId();
    }

    private void executeInsert(UUID id, UUID patId, UUID stfId, UUID brId, UUID bsId,
                               Instant start, Instant end, String status, String serviceName,
                               int duration, BigDecimal fee, String currency) {
        jdbcTemplate.update(
                "INSERT INTO appointment (id, patient_id, staff_id, branch_id, branch_service_id, " +
                        "start_at_utc, end_at_utc, status, service_name_snapshot, duration_minutes_snapshot, " +
                        "fee_amount_snapshot, currency_snapshot, version, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP)",
                id, patId, stfId, brId, bsId,
                Timestamp.from(start), Timestamp.from(end), status, serviceName,
                duration, fee, currency
        );
    }

    @Test
    @DisplayName("Constraint chk_appointment_time_order: Rejects end time equal to or before start time")
    void shouldRejectInvalidTimeOrder() {
        Instant now = Instant.now();
        Instant earlier = now.minus(30, ChronoUnit.MINUTES);

        assertThatThrownBy(() -> executeInsert(
                UUID.randomUUID(), patientId, staffId, branchAId, branchServiceAId,
                now, earlier, "PENDING", "Exam", 30, new BigDecimal("400.00"), "TRY"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Constraint chk_appointment_status: Rejects invalid or lowercase status value")
    void shouldRejectInvalidStatus() {
        Instant now = Instant.now();
        Instant end = now.plus(30, ChronoUnit.MINUTES);

        assertThatThrownBy(() -> executeInsert(
                UUID.randomUUID(), patientId, staffId, branchAId, branchServiceAId,
                now, end, "INVALID_STATUS", "Exam", 30, new BigDecimal("400.00"), "TRY"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Constraint chk_appointment_fee_non_negative: Rejects negative fee amount")
    void shouldRejectNegativeFee() {
        Instant now = Instant.now();
        Instant end = now.plus(30, ChronoUnit.MINUTES);

        assertThatThrownBy(() -> executeInsert(
                UUID.randomUUID(), patientId, staffId, branchAId, branchServiceAId,
                now, end, "PENDING", "Exam", 30, new BigDecimal("-50.00"), "TRY"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Constraint chk_appointment_duration_positive: Rejects duration <= 0 or > 480 min")
    void shouldRejectInvalidDuration() {
        Instant now = Instant.now();
        Instant end = now.plus(30, ChronoUnit.MINUTES);

        assertThatThrownBy(() -> executeInsert(
                UUID.randomUUID(), patientId, staffId, branchAId, branchServiceAId,
                now, end, "PENDING", "Exam", 0, new BigDecimal("400.00"), "TRY"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Constraint chk_appointment_currency_length: Rejects currency code not exactly 3 characters")
    void shouldRejectInvalidCurrencyLength() {
        Instant now = Instant.now();
        Instant end = now.plus(30, ChronoUnit.MINUTES);

        assertThatThrownBy(() -> executeInsert(
                UUID.randomUUID(), patientId, staffId, branchAId, branchServiceAId,
                now, end, "PENDING", "Exam", 30, new BigDecimal("400.00"), "US"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Tenant Integrity Constraint fk_appointment_branch_service_branch: Rejects cross-branch service mismatch at DB level")
    void shouldRejectCrossBranchServiceMismatchAtDatabaseLevel() {
        Instant now = Instant.now();
        Instant end = now.plus(30, ChronoUnit.MINUTES);

        // Intentionally pairing branchAId with branchServiceBId (which belongs to branchB)
        assertThatThrownBy(() -> executeInsert(
                UUID.randomUUID(), patientId, staffId, branchAId, branchServiceBId,
                now, end, "PENDING", "Exam", 30, new BigDecimal("450.00"), "TRY"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }
}
