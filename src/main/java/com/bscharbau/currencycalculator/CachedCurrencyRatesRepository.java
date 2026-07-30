package com.bscharbau.currencycalculator;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface CachedCurrencyRatesRepository extends JpaRepository<CachedCurrencyRates, Long> {

    Optional<CachedCurrencyRates> findByBaseCurrency(String baseCurrency);
}
