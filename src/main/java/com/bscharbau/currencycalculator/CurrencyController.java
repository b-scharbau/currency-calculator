package com.bscharbau.currencycalculator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class CurrencyController {

    private final CurrencyService currencyService;
    private final ExchangeRateService exchangeRateService;

    public CurrencyController(CurrencyService currencyService, ExchangeRateService exchangeRateService) {
        this.currencyService = currencyService;
        this.exchangeRateService = exchangeRateService;
    }

    @Operation(summary = "Get a currency and its current conversion rates",
            description = "Returns a random supported currency by default, or a specific one via the `code` param, "
                    + "along with its exchange rate to every other supported currency.")
    @GetMapping("/currency")
    CurrencyRates currency(
            @Parameter(description = "Currency code to look up, e.g. USD. A random currency is picked if omitted.")
            @RequestParam(required = false) String code) {
        Currency currency = code == null ? randomCurrency() : lookupCurrency(code);
        ExchangeRateService.AllRatesResult rates = exchangeRateService.ratesFor(currency.code());
        List<RateEntry> rateEntries = rates.rates().entrySet().stream()
                .map(entry -> new RateEntry(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(RateEntry::to))
                .toList();
        return new CurrencyRates(currency.code(), currency.name(), rates.date(), rateEntries);
    }

    @Operation(summary = "List all supported currencies", description = "Sourced from Frankfurter and cached in the database.")
    @GetMapping("/currencies")
    List<Currency> currencies() {
        return currencyService.fetchCurrencies();
    }

    private Currency randomCurrency() {
        List<Currency> currencies = currencyService.fetchCurrencies();
        return currencies.get(ThreadLocalRandom.current().nextInt(currencies.size()));
    }

    private Currency lookupCurrency(String code) {
        String upperCode = code.toUpperCase();
        return currencyService.fetchCurrencies().stream()
                .filter(currency -> currency.code().equals(upperCode))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown currency code: " + upperCode));
    }

    record Currency(String code, String name) {
    }

    record CurrencyRates(String code, String name, String date, List<RateEntry> rates) {
    }

    record RateEntry(String to, double rate) {
    }
}
