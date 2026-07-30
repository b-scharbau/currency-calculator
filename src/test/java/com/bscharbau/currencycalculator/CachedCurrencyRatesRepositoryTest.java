package com.bscharbau.currencycalculator;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CachedCurrencyRatesRepositoryTest {

    @Autowired
    private CachedCurrencyRatesRepository repository;

    // ZZA is not a real ISO 4217 code, to avoid colliding with a base currency already cached in the real dev database.

    @Test
    void findsCachedRatesByBaseCurrency() {
        repository.save(new CachedCurrencyRates("ZZA", LocalDate.now(), LocalDate.now(), Map.of("ZZB", 0.9)));

        Optional<CachedCurrencyRates> found = repository.findByBaseCurrency("ZZA");

        assertThat(found).isPresent();
        assertThat(found.get().getRates()).containsEntry("ZZB", 0.9);
    }

    @Test
    void returnsEmptyWhenBaseCurrencyNotCached() {
        Optional<CachedCurrencyRates> found = repository.findByBaseCurrency("XXX");

        assertThat(found).isEmpty();
    }

    @Test
    void baseCurrencyMustBeUnique() {
        repository.save(new CachedCurrencyRates("ZZA", LocalDate.now(), LocalDate.now(), Map.of("ZZB", 0.9)));

        assertThatThrownBy(() -> repository.saveAndFlush(
                new CachedCurrencyRates("ZZA", LocalDate.now(), LocalDate.now(), Map.of("ZZB", 0.5))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
