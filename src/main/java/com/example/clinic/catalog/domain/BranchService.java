package com.example.clinic.catalog.domain;

import com.example.clinic.booking.domain.ServiceSnapshot;
import com.example.clinic.common.domain.BaseEntity;
import com.example.clinic.common.domain.Money;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a medical service offering configured for a specific branch.
 * <p>
 * Invariants:
 * - Owning Branch and associated ServiceDefinition must belong to the exact same Organization.
 * - Service duration must be bounded between 1 and 480 minutes (CAT-02 / ServiceSnapshot).
 * - Service fee must be non-negative with exact 2-decimal financial precision (enforced via Money VO).
 * - Currency must be a valid 3-character ISO code (defaults to TRY).
 * - A branch cannot offer the same ServiceDefinition more than once (enforced via unique constraint).
 */
@Entity
@Table(name = "branch_service", uniqueConstraints = {
        @UniqueConstraint(name = "uq_branch_service_unique", columnNames = {"branch_id", "service_definition_id"})
})
public class BranchService extends BaseEntity {

    public static final int MIN_DURATION_MINUTES = 1;
    public static final int MAX_DURATION_MINUTES = 480;
    public static final String DEFAULT_CURRENCY = "TRY";

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_definition_id", nullable = false)
    private ServiceDefinition serviceDefinition;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @NotNull
    @Column(name = "fee_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal feeAmount;

    @NotNull
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = DEFAULT_CURRENCY;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected BranchService() {
        super();
    }

    public BranchService(Branch branch, ServiceDefinition serviceDefinition, int durationMinutes, Money price) {
        this(UUID.randomUUID(), branch, serviceDefinition, durationMinutes, price, true, Instant.now());
    }

    public BranchService(Organization organization, Branch branch, ServiceDefinition serviceDefinition, int durationMinutes, Money price) {
        this(branch, serviceDefinition, durationMinutes, price);
    }

    public BranchService(Branch branch, ServiceDefinition serviceDefinition, int durationMinutes, BigDecimal feeAmount, String currencyCode) {
        this(branch, serviceDefinition, durationMinutes,
                Money.of(feeAmount, Currency.getInstance(validateCurrencyCode(currencyCode))));
    }

    public BranchService(UUID id, Branch branch, ServiceDefinition serviceDefinition, int durationMinutes, Money price, boolean active, Instant createdAt) {
        super(id, createdAt);
        this.branch = Objects.requireNonNull(branch, "Branch must not be null");
        this.serviceDefinition = Objects.requireNonNull(serviceDefinition, "ServiceDefinition must not be null");

        // Validate cross-tenant domain invariant
        if (!branch.getOrganization().getId().equals(serviceDefinition.getOrganization().getId())) {
            throw new IllegalArgumentException(
                    "Cross-tenant violation: Branch organization (" + branch.getOrganization().getId() +
                    ") does not match ServiceDefinition organization (" + serviceDefinition.getOrganization().getId() + ")"
            );
        }
        this.organization = branch.getOrganization();

        validateDuration(durationMinutes);
        this.durationMinutes = durationMinutes;

        Objects.requireNonNull(price, "Price must not be null");
        this.feeAmount = price.amount();
        this.currency = price.currency().getCurrencyCode();
        this.active = active;
    }

    public BranchService(UUID id, Organization organization, Branch branch, ServiceDefinition serviceDefinition, int durationMinutes, Money price, boolean active, Instant createdAt) {
        this(id, branch, serviceDefinition, durationMinutes, price, active, createdAt);
    }

    private static String validateCurrencyCode(String code) {
        if (code == null || code.isBlank() || code.strip().length() != 3) {
            throw new IllegalArgumentException("Currency code must be a 3-character ISO code: " + code);
        }
        return code.strip().toUpperCase(Locale.ROOT);
    }

    private static void validateDuration(int minutes) {
        if (minutes < MIN_DURATION_MINUTES || minutes > MAX_DURATION_MINUTES) {
            throw new IllegalArgumentException(
                    "durationMinutes must be between " + MIN_DURATION_MINUTES + " and " + MAX_DURATION_MINUTES + ", got: " + minutes
            );
        }
    }

    public Organization getOrganization() {
        return organization;
    }

    public Branch getBranch() {
        return branch;
    }

    public ServiceDefinition getServiceDefinition() {
        return serviceDefinition;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void changeDuration(int newDurationMinutes) {
        validateDuration(newDurationMinutes);
        this.durationMinutes = newDurationMinutes;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public Money getPrice() {
        return Money.of(feeAmount, Currency.getInstance(currency));
    }

    public void changePrice(Money newPrice) {
        Objects.requireNonNull(newPrice, "Price must not be null");
        this.feeAmount = newPrice.amount();
        this.currency = newPrice.currency().getCurrencyCode();
    }

    public void changePrice(BigDecimal newAmount) {
        Objects.requireNonNull(newAmount, "Amount must not be null");
        Money updated = Money.of(newAmount, Currency.getInstance(this.currency));
        this.feeAmount = updated.amount();
    }

    public void changePrice(BigDecimal newAmount, String newCurrencyCode) {
        Objects.requireNonNull(newAmount, "Amount must not be null");
        Money updated = Money.of(newAmount, Currency.getInstance(validateCurrencyCode(newCurrencyCode)));
        this.feeAmount = updated.amount();
        this.currency = updated.currency().getCurrencyCode();
    }

    public boolean isActive() {
        return active;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    /**
     * Bridges S03 service catalog with S06 booking snapshot pattern.
     */
    public ServiceSnapshot toSnapshot() {
        return new ServiceSnapshot(
                this.getId(),
                this.serviceDefinition.getName(),
                this.durationMinutes,
                this.getPrice()
        );
    }

    @Override
    public String toString() {
        return "BranchService{" +
                "id=" + getId() +
                ", branchId=" + (branch != null ? branch.getId() : null) +
                ", serviceDefinitionId=" + (serviceDefinition != null ? serviceDefinition.getId() : null) +
                ", durationMinutes=" + durationMinutes +
                ", feeAmount=" + feeAmount +
                ", currency='" + currency + '\'' +
                ", active=" + active +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}
