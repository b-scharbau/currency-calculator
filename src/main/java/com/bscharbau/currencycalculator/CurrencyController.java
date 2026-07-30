package com.bscharbau.currencycalculator;

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

    @GetMapping("/currency")
    Currency currency() {
        List<Currency> currencies = currencyService.fetchCurrencies();
        return currencies.get(ThreadLocalRandom.current().nextInt(currencies.size()));
    }

    @GetMapping("/currencies")
    List<Currency> currencies() {
        return currencyService.fetchCurrencies();
    }

    record Currency(String code, String name) {
    }
}
