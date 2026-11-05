package com.example.clinic.catalog.domain;

import com.example.clinic.booking.domain.ServiceSnapshot;
import com.example.clinic.common.domain.Money;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BranchServiceTest {

    private Organization organization;
    private Branch branch;
    private ServiceDefinition serviceDefinition;

    @BeforeEach
    void setUp() {
        organization = new Organization("Şehir Hastanesi");
        branch = new Branch(organization, "Kadıköy Şubesi", "Europe/Istanbul", "Bağdat Cad.");
        serviceDefinition = new ServiceDefinition(organization, "Genel Muayene", "Rutin poliklinik muayenesi");
    }

    @Test
    @DisplayName("Should create valid BranchService offering with Money value object")
    void shouldCreateValidBranchServiceWithMoney() {
        Money price = Money.of(new BigDecimal("1500.00"), Currency.getInstance("TRY"));
        BranchService branchService = new BranchService(branch, serviceDefinition, 30, price);

        assertThat(branchService.getId()).isNotNull();
        assertThat(branchService.getBranch()).isEqualTo(branch);
        assertThat(branchService.getServiceDefinition()).isEqualTo(serviceDefinition);
        assertThat(branchService.getOrganization()).isEqualTo(organization);
        assertThat(branchService.getDurationMinutes()).isEqualTo(30);
        assertThat(branchService.getFeeAmount()).isEqualByComparingTo("1500.00");
        assertThat(branchService.getCurrency()).isEqualTo("TRY");
        assertThat(branchService.getPrice()).isEqualTo(price);
        assertThat(branchService.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should create valid BranchService offering with numeric amount and currency string")
    void shouldCreateValidBranchServiceWithNumeric() {
        BranchService branchService = new BranchService(
                branch, serviceDefinition, 45, new BigDecimal("2500.00"), "TRY"
        );

        assertThat(branchService.getDurationMinutes()).isEqualTo(45);
        assertThat(branchService.getFeeAmount()).isEqualByComparingTo("2500.00");
        assertThat(branchService.getCurrency()).isEqualTo("TRY");
    }

    @Test
    @DisplayName("Duration bounds: 1 minute and 480 minutes are valid boundary values")
    void shouldAcceptBoundaryDurations() {
        Money price = Money.of(new BigDecimal("500.00"), Currency.getInstance("TRY"));

        BranchService minDurationService = new BranchService(branch, serviceDefinition, 1, price);
        assertThat(minDurationService.getDurationMinutes()).isEqualTo(1);

        BranchService maxDurationService = new BranchService(branch, serviceDefinition, 480, price);
        assertThat(maxDurationService.getDurationMinutes()).isEqualTo(480);
    }

    @Test
    @DisplayName("Negative: should reject duration below 1 minute (0, negative)")
    void shouldRejectDurationBelowOne() {
        Money price = Money.of(new BigDecimal("500.00"), Currency.getInstance("TRY"));

        assertThatThrownBy(() -> new BranchService(branch, serviceDefinition, 0, price))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("durationMinutes must be between 1 and 480");

        assertThatThrownBy(() -> new BranchService(branch, serviceDefinition, -15, price))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("durationMinutes must be between 1 and 480");
    }

    @Test
    @DisplayName("Negative: should reject duration exceeding 480 minutes")
    void shouldRejectDurationAboveMax() {
        Money price = Money.of(new BigDecimal("500.00"), Currency.getInstance("TRY"));

        assertThatThrownBy(() -> new BranchService(branch, serviceDefinition, 481, price))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("durationMinutes must be between 1 and 480");
    }

    @Test
    @DisplayName("Negative: should reject negative fee via Money invariant")
    void shouldRejectNegativeFee() {
        assertThatThrownBy(() -> new BranchService(branch, serviceDefinition, 30, new BigDecimal("-100.00"), "TRY"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be negative");
    }

    @Test
    @DisplayName("Negative: should reject fractional precision beyond 2 decimal places")
    void shouldRejectUnsupportedPrecision() {
        assertThatThrownBy(() -> new BranchService(branch, serviceDefinition, 30, new BigDecimal("100.555"), "TRY"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported fractional precision");
    }

    @Test
    @DisplayName("Negative: should reject invalid currency code")
    void shouldRejectInvalidCurrencyCode() {
        assertThatThrownBy(() -> new BranchService(branch, serviceDefinition, 30, new BigDecimal("100.00"), "INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Negative: should reject cross-tenant pairing between branch and service definition")
    void shouldRejectCrossTenantPairing() {
        Organization otherOrg = new Organization("Başka Kurum");
        ServiceDefinition otherServiceDef = new ServiceDefinition(otherOrg, "Genel Cerrahi", "Açıklama");
        Money price = Money.of(new BigDecimal("1000.00"), Currency.getInstance("TRY"));

        assertThatThrownBy(() -> new BranchService(branch, otherServiceDef, 30, price))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cross-tenant violation");
    }

    @Test
    @DisplayName("Should update price maintaining exact precision and currency")
    void shouldUpdatePrice() {
        Money initialPrice = Money.of(new BigDecimal("800.00"), Currency.getInstance("TRY"));
        BranchService branchService = new BranchService(branch, serviceDefinition, 30, initialPrice);

        branchService.changePrice(new BigDecimal("950.00"));
        assertThat(branchService.getFeeAmount()).isEqualByComparingTo("950.00");
        assertThat(branchService.getCurrency()).isEqualTo("TRY");

        Money newPrice = Money.of(new BigDecimal("1200.00"), Currency.getInstance("TRY"));
        branchService.changePrice(newPrice);
        assertThat(branchService.getPrice()).isEqualTo(newPrice);

        assertThatThrownBy(() -> branchService.changePrice(new BigDecimal("-50.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should update duration and validate new duration range")
    void shouldUpdateDuration() {
        Money price = Money.of(new BigDecimal("800.00"), Currency.getInstance("TRY"));
        BranchService branchService = new BranchService(branch, serviceDefinition, 30, price);

        branchService.changeDuration(45);
        assertThat(branchService.getDurationMinutes()).isEqualTo(45);

        assertThatThrownBy(() -> branchService.changeDuration(0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> branchService.changeDuration(500))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should manage active lifecycle transitions")
    void shouldManageLifecycle() {
        Money price = Money.of(new BigDecimal("800.00"), Currency.getInstance("TRY"));
        BranchService branchService = new BranchService(branch, serviceDefinition, 30, price);

        assertThat(branchService.isActive()).isTrue();
        branchService.deactivate();
        assertThat(branchService.isActive()).isFalse();
        branchService.activate();
        assertThat(branchService.isActive()).isTrue();
    }

    @Test
    @DisplayName("Snapshot: should bridge catalog offering to immutable ServiceSnapshot for S06 appointments")
    void shouldConvertToServiceSnapshot() {
        Money price = Money.of(new BigDecimal("1750.00"), Currency.getInstance("TRY"));
        BranchService branchService = new BranchService(branch, serviceDefinition, 30, price);

        ServiceSnapshot snapshot = branchService.toSnapshot();

        assertThat(snapshot.getServiceId()).isEqualTo(branchService.getId());
        assertThat(snapshot.getServiceName()).isEqualTo("Genel Muayene");
        assertThat(snapshot.getDurationMinutes()).isEqualTo(30);
        assertThat(snapshot.getFee()).isEqualTo(price);
    }
}
