package com.bscharbau.currencycalculator;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CurrencyService {

    private final RestClient restClient;
    private final CurrencyRepository repository;

    public CurrencyService(RestClient frankfurterRestClient, CurrencyRepository repository) {
        this.restClient = frankfurterRestClient;
        this.repository = repository;
    }

    @PostConstruct
    void seedCurrenciesIfEmpty() {
        if (repository.count() > 0) {
            return;
        }

        Map<String, String> currencies = FrankfurterRetry.execute(() -> restClient.get()
                .uri("/currencies")
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, String>>() {
                }));

        List<CurrencyEntity> entities = currencies.entrySet().stream()
                .map(entry -> new CurrencyEntity(entry.getKey(), entry.getValue()))
                .toList();
        repository.saveAll(entities);
    }

    List<CurrencyController.Currency> fetchCurrencies() {
        return repository.findAllByOrderByCodeAsc().stream()
                .map(entity -> new CurrencyController.Currency(entity.getCode(), entity.getName()))
                .toList();
    }
}
