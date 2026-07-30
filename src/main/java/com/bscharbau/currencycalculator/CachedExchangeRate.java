package com.bscharbau.currencycalculator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

@Entity
@Table(name = "cached_exchange_rate", uniqueConstraints = @UniqueConstraint(columnNames = {"from_currency", "to_currency"}))
public class CachedExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_currency", nullable = false)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false)
    private String toCurrency;

    @Column(nullable = false)
    private double rate;

    @Column(nullable = false)
    private LocalDate rateDate;

    @Column(nullable = false)
    private LocalDate fetchedAt;

    protected CachedExchangeRate() {
    }

    CachedExchangeRate(String fromCurrency, String toCurrency, double rate, LocalDate rateDate, LocalDate fetchedAt) {
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.rate = rate;
        this.rateDate = rateDate;
        this.fetchedAt = fetchedAt;
    }

    Long getId() {
        return id;
    }

    String getFromCurrency() {
        return fromCurrency;
    }

    String getToCurrency() {
        return toCurrency;
    }

    double getRate() {
        return rate;
    }

    LocalDate getRateDate() {
        return rateDate;
    }

    LocalDate getFetchedAt() {
        return fetchedAt;
    }

    void update(double rate, LocalDate rateDate, LocalDate fetchedAt) {
        this.rate = rate;
        this.rateDate = rateDate;
        this.fetchedAt = fetchedAt;
    }
}
