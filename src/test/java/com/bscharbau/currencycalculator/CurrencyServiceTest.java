package com.bscharbau.currencycalculator;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CurrencyServiceTest {

    private static final String BASE_URL = "https://api.frankfurter.dev/v1";

    private MockRestServiceServer server;
    private CurrencyRepository repository;
    private CurrencyService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        repository = mock(CurrencyRepository.class);
        service = new CurrencyService(restClient, repository);
    }

    @Test
    void seedsFromApiWhenTableIsEmpty() {
        given(repository.count()).willReturn(0L);
        server.expect(requestTo(BASE_URL + "/currencies"))
                .andRespond(withSuccess("{\"EUR\":\"Euro\",\"USD\":\"US Dollar\"}", MediaType.APPLICATION_JSON));

        service.seedCurrenciesIfEmpty();

        server.verify();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CurrencyEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(CurrencyEntity::getCode).containsExactlyInAnyOrder("EUR", "USD");
        assertThat(captor.getValue()).extracting(CurrencyEntity::getName).containsExactlyInAnyOrder("Euro", "US Dollar");
    }

    @Test
    void skipsApiCallWhenTableAlreadyPopulated() {
        given(repository.count()).willReturn(30L);

        service.seedCurrenciesIfEmpty();

        verify(repository, never()).saveAll(any());
    }

    @Test
    void retriesSeedOnTransientFailureThenSucceeds() {
        given(repository.count()).willReturn(0L);
        server.expect(requestTo(BASE_URL + "/currencies")).andRespond(withServerError());
        server.expect(requestTo(BASE_URL + "/currencies"))
                .andRespond(withSuccess("{\"JPY\":\"Japanese Yen\"}", MediaType.APPLICATION_JSON));

        service.seedCurrenciesIfEmpty();

        server.verify();
        verify(repository).saveAll(anyList());
    }

    @Test
    void fetchCurrenciesMapsRepositoryEntitiesToDtos() {
        given(repository.findAllByOrderByCodeAsc()).willReturn(List.of(
                new CurrencyEntity("EUR", "Euro"),
                new CurrencyEntity("USD", "US Dollar")
        ));

        List<CurrencyController.Currency> result = service.fetchCurrencies();

        assertThat(result).containsExactly(
                new CurrencyController.Currency("EUR", "Euro"),
                new CurrencyController.Currency("USD", "US Dollar")
        );
    }
}
