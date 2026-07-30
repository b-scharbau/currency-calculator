package com.bscharbau.currencycalculator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CurrencyConversionController {

    private final ExchangeRateService exchangeRateService;

    public CurrencyConversionController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @Operation(summary = "Convert an amount between two currencies",
            description = "Uses today's cached exchange rate for the pair, fetching a fresh rate from Frankfurter if none is cached yet.")
    @GetMapping("/convert")
    ConversionResult convert(
            @Parameter(description = "Currency code to convert from, e.g. USD", example = "USD") @RequestParam String from,
            @Parameter(description = "Currency code to convert to, e.g. EUR", example = "EUR") @RequestParam String to,
            @Parameter(description = "Amount in the from currency", example = "100") @RequestParam double amount) {
        return exchangeRateService.convert(from, to, amount);
    }

    record ConversionResult(String from, String to, double amount, double convertedAmount, double rate, String date) {
    }
}
