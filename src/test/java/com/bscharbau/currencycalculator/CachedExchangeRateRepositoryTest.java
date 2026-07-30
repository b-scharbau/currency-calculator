package com.bscharbau.currencycalculator;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CachedExchangeRateRepositoryTest {

    @Autowired
    private CachedExchangeRateRepository repository;

    // ZZA/ZZB are not real ISO 4217 codes, to avoid colliding with pairs already cached in the real dev database.

    @Test
    void findsCachedRateByCurrencyPair() {
        repository.save(new CachedExchangeRate("ZZA", "ZZB", 0.9, LocalDate.now(), LocalDate.now()));

        Optional<CachedExchangeRate> found = repository.findByFromCurrencyAndToCurrency("ZZA", "ZZB");

        assertThat(found).isPresent();
        assertThat(found.get().getRate()).isCloseTo(0.9, within(1e-9));
    }

    @Test
    void returnsEmptyWhenPairNotCached() {
        Optional<CachedExchangeRate> found = repository.findByFromCurrencyAndToCurrency("XXX", "YYY");

        assertThat(found).isEmpty();
    }

    @Test
    void currencyPairMustBeUnique() {
        repository.save(new CachedExchangeRate("ZZA", "ZZB", 0.9, LocalDate.now(), LocalDate.now()));

        assertThatThrownBy(() -> repository.saveAndFlush(
                new CachedExchangeRate("ZZA", "ZZB", 0.5, LocalDate.now(), LocalDate.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
