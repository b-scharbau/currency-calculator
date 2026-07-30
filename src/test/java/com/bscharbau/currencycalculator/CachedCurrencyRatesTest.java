package com.bscharbau.currencycalculator;

import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CachedCurrencyRatesTest {

    @Test
    void updateReplacesRateDateFetchedAtAndRatesButKeepsBaseCurrency() {
        CachedCurrencyRates rates = new CachedCurrencyRates(
                "USD", LocalDate.of(2026, 7, 28), LocalDate.of(2026, 7, 28), Map.of("EUR", 0.85));

        rates.update(LocalDate.of(2026, 7, 29), LocalDate.of(2026, 7, 29), Map.of("EUR", 0.9, "JPY", 149.5));

        assertThat(rates.getBaseCurrency()).isEqualTo("USD");
        assertThat(rates.getRateDate()).isEqualTo(LocalDate.of(2026, 7, 29));
        assertThat(rates.getFetchedAt()).isEqualTo(LocalDate.of(2026, 7, 29));
        assertThat(rates.getRates()).containsExactlyInAnyOrderEntriesOf(Map.of("EUR", 0.9, "JPY", 149.5));
    }
}
