package com.bscharbau.currencycalculator;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CurrencyRepositoryTest {

    @Autowired
    private CurrencyRepository repository;

    // Codes here are deliberately not real ISO 4217 codes: this test runs against the real dev
    // database (see class-level @AutoConfigureTestDatabase), which may already have genuine
    // currencies like USD/EUR/AUD seeded, and those would collide with the unique constraint.

    @Test
    void findAllByOrderByCodeAscReturnsCurrenciesSortedByCode() {
        repository.saveAll(List.of(
                new CurrencyEntity("ZZC", "Test Currency C"),
                new CurrencyEntity("ZZA", "Test Currency A"),
                new CurrencyEntity("ZZB", "Test Currency B")
        ));

        List<String> testCodesInReturnedOrder = repository.findAllByOrderByCodeAsc().stream()
                .map(CurrencyEntity::getCode)
                .filter(code -> code.startsWith("ZZ"))
                .toList();

        assertThat(testCodesInReturnedOrder).containsExactly("ZZA", "ZZB", "ZZC");
    }

    @Test
    void codeMustBeUnique() {
        repository.save(new CurrencyEntity("ZZD", "Test Currency D"));

        assertThatThrownBy(() -> repository.saveAndFlush(new CurrencyEntity("ZZD", "Test Currency D (duplicate)")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
