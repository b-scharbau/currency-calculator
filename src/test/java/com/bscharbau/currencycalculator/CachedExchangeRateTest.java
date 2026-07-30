package com.bscharbau.currencycalculator;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CachedExchangeRateTest {

    @Test
    void updateReplacesRateDateAndFetchedAtButKeepsCurrencyPair() {
        CachedExchangeRate rate = new CachedExchangeRate(
                "USD", "EUR", 0.85, LocalDate.of(2026, 7, 28), LocalDate.of(2026, 7, 28));

        rate.update(0.9, LocalDate.of(2026, 7, 29), LocalDate.of(2026, 7, 29));

        assertThat(rate.getFromCurrency()).isEqualTo("USD");
        assertThat(rate.getToCurrency()).isEqualTo("EUR");
        assertThat(rate.getRate()).isCloseTo(0.9, within(1e-9));
        assertThat(rate.getRateDate()).isEqualTo(LocalDate.of(2026, 7, 29));
        assertThat(rate.getFetchedAt()).isEqualTo(LocalDate.of(2026, 7, 29));
    }
}
