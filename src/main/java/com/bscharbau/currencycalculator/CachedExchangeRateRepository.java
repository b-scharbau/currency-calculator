package com.bscharbau.currencycalculator;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface CachedExchangeRateRepository extends JpaRepository<CachedExchangeRate, Long> {

    Optional<CachedExchangeRate> findByFromCurrencyAndToCurrency(String fromCurrency, String toCurrency);
}
