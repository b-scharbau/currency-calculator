package com.bscharbau.currencycalculator;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CurrencyConversionController {

    private final ExchangeRateService exchangeRateService;

    public CurrencyConversionController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping("/convert")
    ConversionResult convert(@RequestParam String from, @RequestParam String to, @RequestParam double amount) {
        return exchangeRateService.convert(from, to, amount);
    }

    record ConversionResult(String from, String to, double amount, double convertedAmount, double rate, String date) {
    }
}
