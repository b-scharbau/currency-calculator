package com.bscharbau.currencycalculator;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExchangeRateService {

    private final RestClient restClient = RestClient.create("https://api.frankfurter.dev/v1");
    private final CachedExchangeRateRepository repository;

    public ExchangeRateService(CachedExchangeRateRepository repository) {
        this.repository = repository;
    }

    CurrencyConversionController.ConversionResult convert(String from, String to, double amount) {
        String fromCode = from.toUpperCase();
        String toCode = to.toUpperCase();

        RateInfo rateInfo = resolveRate(fromCode, toCode);
        double convertedAmount = amount * rateInfo.rate();
        return new CurrencyConversionController.ConversionResult(
                fromCode, toCode, amount, convertedAmount, rateInfo.rate(), rateInfo.date().toString());
    }

    private RateInfo resolveRate(String fromCode, String toCode) {
        Optional<CachedExchangeRate> cached = repository.findByFromCurrencyAndToCurrency(fromCode, toCode);
        LocalDate today = LocalDate.now();
        if (cached.isPresent() && cached.get().getFetchedAt().equals(today)) {
            return new RateInfo(cached.get().getRate(), cached.get().getRateDate());
        }

        FrankfurterResponse response = fetchRate(fromCode, toCode);
        Double rate = response == null ? null : response.rates().get(toCode);
        if (rate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown currency code: " + toCode);
        }
        LocalDate rateDate = LocalDate.parse(response.date());

        if (cached.isPresent()) {
            cached.get().update(rate, rateDate, today);
            repository.save(cached.get());
        } else {
            repository.save(new CachedExchangeRate(fromCode, toCode, rate, rateDate, today));
        }
        return new RateInfo(rate, rateDate);
    }

    private FrankfurterResponse fetchRate(String fromCode, String toCode) {
        try {
            return restClient.get()
                    .uri("/latest?amount=1&from={from}&to={to}", fromCode, toCode)
                    .retrieve()
                    .body(FrankfurterResponse.class);
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown currency code: " + fromCode + " or " + toCode, e);
        }
    }

    private record RateInfo(double rate, LocalDate date) {
    }

    private record FrankfurterResponse(double amount, String base, String date, Map<String, Double> rates) {
    }
}
