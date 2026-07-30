package com.bscharbau.currencycalculator;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExchangeRateService {

    private final RestClient restClient = RestClient.create("https://api.frankfurter.dev/v1");

    CurrencyConversionController.ConversionResult convert(String from, String to, double amount) {
        String fromCode = from.toUpperCase();
        String toCode = to.toUpperCase();

        FrankfurterResponse response;
        try {
            response = restClient.get()
                    .uri("/latest?amount={amount}&from={from}&to={to}", amount, fromCode, toCode)
                    .retrieve()
                    .body(FrankfurterResponse.class);
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown currency code: " + fromCode + " or " + toCode, e);
        }

        Double convertedAmount = response == null ? null : response.rates().get(toCode);
        if (convertedAmount == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown currency code: " + toCode);
        }

        double rate = amount == 0 ? 0 : convertedAmount / amount;
        return new CurrencyConversionController.ConversionResult(fromCode, toCode, amount, convertedAmount, rate, response.date());
    }

    private record FrankfurterResponse(double amount, String base, String date, Map<String, Double> rates) {
    }
}
