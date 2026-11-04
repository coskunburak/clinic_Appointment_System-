package com.example.clinic.common.domain;

import com.example.clinic.common.exception.CurrencyMismatchException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    private static final Currency TRY = Currency.getInstance("TRY");
    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");

    @Test
    @DisplayName("Positive: Zero amount is accepted and normalized to 2 decimal places")
    void shouldAcceptZeroAmount() {
        Money zero = Money.zero(TRY);
        assertThat(zero.amount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(zero.currency()).isEqualTo(TRY);
        assertThat(zero.isZero()).isTrue();
        assertThat(zero.isPositive()).isFalse();
    }

    @Test
    @DisplayName("Positive: Valid positive amount is accepted and normalized")
    void shouldAcceptPositiveAmount() {
        Money money = Money.of(new BigDecimal("150.5"), TRY);
        assertThat(money.amount()).isEqualTo(new BigDecimal("150.50"));
        assertThat(money.currency()).isEqualTo(TRY);
        assertThat(money.isPositive()).isTrue();
        assertThat(money.isZero()).isFalse();
    }

    @Test
    @DisplayName("Negative: Null amount is rejected with NullPointerException")
    void shouldRejectNullAmount() {
        assertThatThrownBy(() -> Money.of((BigDecimal) null, TRY))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Amount must not be null");
    }

    @Test
    @DisplayName("Negative: Null currency is rejected with NullPointerException")
    void shouldRejectNullCurrency() {
        assertThatThrownBy(() -> Money.of(BigDecimal.TEN, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Currency must not be null");
    }

    @Test
    @DisplayName("Negative: Negative amount is rejected with IllegalArgumentException")
    void shouldRejectNegativeAmount() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("-0.01"), TRY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be negative");
    }

    @Test
    @DisplayName("Positive: Same-currency addition produces correct sum without mutating operands")
    void shouldAddSameCurrency() {
        Money m1 = Money.of(new BigDecimal("100.25"), TRY);
        Money m2 = Money.of(new BigDecimal("50.75"), TRY);

        Money sum = m1.add(m2);

        assertThat(sum.amount()).isEqualTo(new BigDecimal("151.00"));
        assertThat(sum.currency()).isEqualTo(TRY);

        // Immutability check
        assertThat(m1.amount()).isEqualTo(new BigDecimal("100.25"));
        assertThat(m2.amount()).isEqualTo(new BigDecimal("50.75"));
    }

    @Test
    @DisplayName("Negative: Different-currency addition is rejected with CurrencyMismatchException")
    void shouldRejectAdditionOfDifferentCurrencies() {
        Money tryMoney = Money.of(new BigDecimal("100.00"), TRY);
        Money usdMoney = Money.of(new BigDecimal("100.00"), USD);

        assertThatThrownBy(() -> tryMoney.add(usdMoney))
                .isInstanceOf(CurrencyMismatchException.class)
                .hasMessageContaining("TRY vs USD");
    }

    @Test
    @DisplayName("Positive: Same-currency subtraction produces correct difference")
    void shouldSubtractSameCurrency() {
        Money m1 = Money.of(new BigDecimal("100.50"), EUR);
        Money m2 = Money.of(new BigDecimal("40.20"), EUR);

        Money diff = m1.subtract(m2);

        assertThat(diff.amount()).isEqualTo(new BigDecimal("60.30"));
        assertThat(diff.currency()).isEqualTo(EUR);
    }

    @Test
    @DisplayName("Negative: Subtraction resulting in negative amount is rejected")
    void shouldRejectSubtractionYieldingNegativeResult() {
        Money m1 = Money.of(new BigDecimal("50.00"), TRY);
        Money m2 = Money.of(new BigDecimal("50.01"), TRY);

        assertThatThrownBy(() -> m1.subtract(m2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative monetary amount");
    }

    @Test
    @DisplayName("Negative: Subtraction across different currencies is rejected")
    void shouldRejectSubtractionOfDifferentCurrencies() {
        Money tryMoney = Money.of(new BigDecimal("100.00"), TRY);
        Money eurMoney = Money.of(new BigDecimal("50.00"), EUR);

        assertThatThrownBy(() -> tryMoney.subtract(eurMoney))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    @DisplayName("Positive: Same-currency multiplication produces normalized result")
    void shouldMultiplyByValidFactor() {
        Money m = Money.of(new BigDecimal("25.00"), USD);
        Money result = m.multiply(new BigDecimal("1.5"));

        assertThat(result.amount()).isEqualTo(new BigDecimal("37.50"));
        assertThat(result.currency()).isEqualTo(USD);
    }

    @Test
    @DisplayName("Negative: Negative multiplication factor is rejected")
    void shouldRejectNegativeMultiplicationFactor() {
        Money m = Money.of(new BigDecimal("25.00"), USD);
        assertThatThrownBy(() -> m.multiply(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be negative");
    }

    @Test
    @DisplayName("Negative: Multiplication resulting in unsupported fractional precision is rejected")
    void shouldRejectMultiplicationWithExcessPrecision() {
        Money m = Money.of(new BigDecimal("10.00"), TRY);
        assertThatThrownBy(() -> m.multiply(new BigDecimal("1.3333")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported fractional precision");
    }

    @Test
    @DisplayName("Boundary: Unsupported fractional precision in constructor is rejected without silent rounding")
    void shouldRejectUnsupportedPrecisionInConstructor() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("10.005"), TRY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported fractional precision");
    }

    @Test
    @DisplayName("Boundary: Scale normalization converts 10 and 10.0 to 10.00 consistently")
    void shouldNormalizeScaleConsistently() {
        Money m1 = Money.of(new BigDecimal("10"), TRY);
        Money m2 = Money.of(new BigDecimal("10.0"), TRY);
        Money m3 = Money.of(new BigDecimal("10.00"), TRY);

        assertThat(m1).isEqualTo(m2);
        assertThat(m2).isEqualTo(m3);
        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
        assertThat(m2.hashCode()).isEqualTo(m3.hashCode());
    }

    @Test
    @DisplayName("Positive: String-based construction succeeds and matches BigDecimal construction")
    void shouldConstructFromString() {
        Money m = Money.of("250.75", TRY);
        assertThat(m.amount()).isEqualTo(new BigDecimal("250.75"));
        assertThat(m.currency()).isEqualTo(TRY);
        assertThat(m.toString()).isEqualTo("250.75 TRY");
    }

    @Test
    @DisplayName("Positive: CompareTo correctly orders amounts with same currency")
    void shouldCompareMonetaryAmounts() {
        Money m1 = Money.of(new BigDecimal("10.00"), TRY);
        Money m2 = Money.of(new BigDecimal("20.00"), TRY);

        assertThat(m1.compareTo(m2)).isNegative();
        assertThat(m2.compareTo(m1)).isPositive();
        assertThat(m1.compareTo(Money.of("10.00", TRY))).isZero();
    }
}
