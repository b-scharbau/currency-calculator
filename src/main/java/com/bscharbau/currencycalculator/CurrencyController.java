package com.bscharbau.currencycalculator;

import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CurrencyController {

    private final CurrencyService currencyService;

    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @Operation(summary = "Get a random supported currency")
    @GetMapping("/currency")
    Currency currency() {
        List<Currency> currencies = currencyService.fetchCurrencies();
        return currencies.get(ThreadLocalRandom.current().nextInt(currencies.size()));
    }

    @Operation(summary = "List all supported currencies", description = "Sourced from Frankfurter and cached in the database.")
    @GetMapping("/currencies")
    List<Currency> currencies() {
        return currencyService.fetchCurrencies();
    }

    record Currency(String code, String name) {
    }
}
