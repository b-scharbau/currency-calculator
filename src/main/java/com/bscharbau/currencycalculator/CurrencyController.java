package com.bscharbau.currencycalculator;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CurrencyController {

    private static final List<Currency> CURRENCIES = List.of(
            new Currency("USD", "US Dollar"),
            new Currency("EUR", "Euro"),
            new Currency("GBP", "British Pound"),
            new Currency("JPY", "Japanese Yen"),
            new Currency("CHF", "Swiss Franc"),
            new Currency("AUD", "Australian Dollar"),
            new Currency("CAD", "Canadian Dollar"),
            new Currency("CNY", "Chinese Yuan"),
            new Currency("NZD", "New Zealand Dollar"),
            new Currency("SEK", "Swedish Krona")
    );

    @GetMapping("/currency")
    Currency currency() {
        return CURRENCIES.get(ThreadLocalRandom.current().nextInt(CURRENCIES.size()));
    }

    @GetMapping("/currencies")
    List<Currency> currencies() {
        return CURRENCIES;
    }

    record Currency(String code, String name) {
    }
}
