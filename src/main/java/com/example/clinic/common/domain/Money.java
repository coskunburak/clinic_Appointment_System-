package com.example.clinic.common.domain;

import com.example.clinic.common.exception.CurrencyMismatchException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Immutable monetary value object adhering to DDD and financial precision rules.
 * <p>
 * Enforces non-negative amounts, exact 2-decimal scale normalization without silent rounding,
 * and currency-safe arithmetic operations.
 */
public final class Money implements Comparable<Money> {

    public static final int DEFAULT_SCALE = 2;

    private final BigDecimal amount;
    private final Currency currency;

    private Money(BigDecimal amount, Currency currency) {
        Objects.requireNonNull(currency, "Currency must not be null");
        Objects.requireNonNull(amount, "Amount must not be null");

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Monetary amount cannot be negative: " + amount);
        }

        // Validate that fractional precision beyond 2 decimal places is not silently truncated
        BigDecimal stripped = amount.stripTrailingZeros();
        if (stripped.scale() > DEFAULT_SCALE) {
            throw new IllegalArgumentException(
                    "Unsupported fractional precision beyond " + DEFAULT_SCALE + " decimal places: " + amount
            );
        }

        this.amount = amount.setScale(DEFAULT_SCALE, RoundingMode.UNNECESSARY);
        this.currency = currency;
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money of(String amountStr, Currency currency) {
        Objects.requireNonNull(amountStr, "Amount string must not be null");
        return new Money(new BigDecimal(amountStr), currency);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public BigDecimal amount() {
        return amount;
    }

    public Currency currency() {
        return currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Money add(Money other) {
        checkSameCurrency(other);
        BigDecimal newAmount = this.amount.add(other.amount);
        return new Money(newAmount, this.currency);
    }

    public Money subtract(Money other) {
        checkSameCurrency(other);
        BigDecimal newAmount = this.amount.subtract(other.amount);
        if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Subtraction resulting in a negative monetary amount is rejected: " + newAmount
            );
        }
        return new Money(newAmount, this.currency);
    }

    public Money multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "Multiplication factor must not be null");
        if (factor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Multiplication factor cannot be negative: " + factor);
        }
        BigDecimal rawResult = this.amount.multiply(factor);
        BigDecimal stripped = rawResult.stripTrailingZeros();
        if (stripped.scale() > DEFAULT_SCALE) {
            throw new IllegalArgumentException(
                    "Multiplication produced unsupported fractional precision beyond " + DEFAULT_SCALE + " decimal places: " + rawResult
            );
        }
        return new Money(rawResult.setScale(DEFAULT_SCALE, RoundingMode.UNNECESSARY), this.currency);
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    private void checkSameCurrency(Money other) {
        Objects.requireNonNull(other, "Money operand must not be null");
        if (!this.currency.equals(other.currency)) {
            throw new CurrencyMismatchException(
                    "Cannot perform monetary operations across distinct currencies: " +
                            this.currency.getCurrencyCode() + " vs " + other.currency.getCurrencyCode()
            );
        }
    }

    @Override
    public int compareTo(Money other) {
        checkSameCurrency(other);
        return this.amount.compareTo(other.amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return this.currency.equals(money.currency) && this.amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }
}
