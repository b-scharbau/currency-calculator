package com.bscharbau.currencycalculator;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "cached_currency_rates", uniqueConstraints = @UniqueConstraint(columnNames = "base_currency"))
public class CachedCurrencyRates {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "base_currency", nullable = false)
    private String baseCurrency;

    @Column(nullable = false)
    private LocalDate rateDate;

    @Column(nullable = false)
    private LocalDate fetchedAt;

    @ElementCollection
    @CollectionTable(name = "cached_currency_rate_entry", joinColumns = @JoinColumn(name = "cached_currency_rates_id"))
    @MapKeyColumn(name = "target_currency")
    @Column(name = "rate", nullable = false)
    private Map<String, Double> rates = new HashMap<>();

    protected CachedCurrencyRates() {
    }

    CachedCurrencyRates(String baseCurrency, LocalDate rateDate, LocalDate fetchedAt, Map<String, Double> rates) {
        this.baseCurrency = baseCurrency;
        this.rateDate = rateDate;
        this.fetchedAt = fetchedAt;
        this.rates = new HashMap<>(rates);
    }

    Long getId() {
        return id;
    }

    String getBaseCurrency() {
        return baseCurrency;
    }

    LocalDate getRateDate() {
        return rateDate;
    }

    LocalDate getFetchedAt() {
        return fetchedAt;
    }

    Map<String, Double> getRates() {
        return rates;
    }

    void update(LocalDate rateDate, LocalDate fetchedAt, Map<String, Double> rates) {
        this.rateDate = rateDate;
        this.fetchedAt = fetchedAt;
        this.rates.clear();
        this.rates.putAll(rates);
    }
}
