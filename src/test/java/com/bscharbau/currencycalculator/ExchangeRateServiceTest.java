package com.bscharbau.currencycalculator;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ExchangeRateServiceTest {

    private static final String BASE_URL = "https://api.frankfurter.dev/v1";

    private MockRestServiceServer server;
    private CachedExchangeRateRepository repository;
    private ExchangeRateService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        repository = mock(CachedExchangeRateRepository.class);
        service = new ExchangeRateService(restClient, repository);
    }

    @Test
    void fetchesFromApiAndCachesWhenNoCacheEntryExists() {
        given(repository.findByFromCurrencyAndToCurrency("USD", "EUR")).willReturn(Optional.empty());
        server.expect(requestTo(BASE_URL + "/latest?amount=1&from=USD&to=EUR"))
                .andRespond(withSuccess("{\"amount\":1.0,\"base\":\"USD\",\"date\":\"2026-07-29\",\"rates\":{\"EUR\":0.9}}", MediaType.APPLICATION_JSON));

        var result = service.convert("usd", "eur", 10.0);

        assertThat(result.from()).isEqualTo("USD");
        assertThat(result.to()).isEqualTo("EUR");
        assertThat(result.amount()).isEqualTo(10.0);
        assertThat(result.convertedAmount()).isCloseTo(9.0, within(1e-9));
        assertThat(result.rate()).isCloseTo(0.9, within(1e-9));
        assertThat(result.date()).isEqualTo("2026-07-29");
        server.verify();
        verify(repository).save(any(CachedExchangeRate.class));
    }

    @Test
    void usesCachedRateWhenAlreadyFetchedToday() {
        CachedExchangeRate cached = new CachedExchangeRate("USD", "EUR", 0.8, LocalDate.now(), LocalDate.now());
        given(repository.findByFromCurrencyAndToCurrency("USD", "EUR")).willReturn(Optional.of(cached));

        var result = service.convert("USD", "EUR", 5.0);

        assertThat(result.convertedAmount()).isCloseTo(4.0, within(1e-9));
        server.verify();
        verify(repository, never()).save(any());
    }

    @Test
    void refetchesAndUpdatesCacheWhenEntryIsStale() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        CachedExchangeRate cached = new CachedExchangeRate("USD", "EUR", 0.85, yesterday, yesterday);
        given(repository.findByFromCurrencyAndToCurrency("USD", "EUR")).willReturn(Optional.of(cached));
        server.expect(requestTo(BASE_URL + "/latest?amount=1&from=USD&to=EUR"))
                .andRespond(withSuccess("{\"amount\":1.0,\"base\":\"USD\",\"date\":\"2026-07-29\",\"rates\":{\"EUR\":0.95}}", MediaType.APPLICATION_JSON));

        var result = service.convert("USD", "EUR", 2.0);

        assertThat(result.rate()).isCloseTo(0.95, within(1e-9));
        server.verify();
        verify(repository).save(cached);
        assertThat(cached.getRate()).isCloseTo(0.95, within(1e-9));
        assertThat(cached.getFetchedAt()).isEqualTo(LocalDate.now());
    }

    @Test
    void throwsBadRequestWhenResponseHasNoRateForTargetCurrency() {
        given(repository.findByFromCurrencyAndToCurrency("USD", "ZZZ")).willReturn(Optional.empty());
        server.expect(requestTo(BASE_URL + "/latest?amount=1&from=USD&to=ZZZ"))
                .andRespond(withSuccess("{\"amount\":1.0,\"base\":\"USD\",\"date\":\"2026-07-29\",\"rates\":{}}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.convert("USD", "ZZZ", 1.0))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);
    }

    @Test
    void throwsBadRequestImmediatelyWhenApiRejectsUnknownFromCurrency() {
        given(repository.findByFromCurrencyAndToCurrency("XXX", "EUR")).willReturn(Optional.empty());
        server.expect(requestTo(BASE_URL + "/latest?amount=1&from=XXX&to=EUR"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).body("not found").contentType(MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> service.convert("XXX", "EUR", 1.0))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);
        server.verify();
    }

    @Test
    void retriesOnTransientServerErrorThenSucceeds() {
        given(repository.findByFromCurrencyAndToCurrency("USD", "EUR")).willReturn(Optional.empty());
        server.expect(requestTo(BASE_URL + "/latest?amount=1&from=USD&to=EUR")).andRespond(withServerError());
        server.expect(requestTo(BASE_URL + "/latest?amount=1&from=USD&to=EUR"))
                .andRespond(withSuccess("{\"amount\":1.0,\"base\":\"USD\",\"date\":\"2026-07-29\",\"rates\":{\"EUR\":0.9}}", MediaType.APPLICATION_JSON));

        var result = service.convert("USD", "EUR", 1.0);

        assertThat(result.rate()).isCloseTo(0.9, within(1e-9));
        server.verify();
    }

    @Test
    void givesUpAfterExhaustingRetriesOnPersistentServerErrors() {
        given(repository.findByFromCurrencyAndToCurrency("USD", "EUR")).willReturn(Optional.empty());
        for (int i = 0; i < 3; i++) {
            server.expect(requestTo(BASE_URL + "/latest?amount=1&from=USD&to=EUR")).andRespond(withServerError());
        }

        assertThatThrownBy(() -> service.convert("USD", "EUR", 1.0))
                .isInstanceOf(HttpServerErrorException.class);
        server.verify();
    }

    @Test
    void handlesZeroAmount() {
        given(repository.findByFromCurrencyAndToCurrency("USD", "EUR")).willReturn(Optional.empty());
        server.expect(requestTo(BASE_URL + "/latest?amount=1&from=USD&to=EUR"))
                .andRespond(withSuccess("{\"amount\":1.0,\"base\":\"USD\",\"date\":\"2026-07-29\",\"rates\":{\"EUR\":0.9}}", MediaType.APPLICATION_JSON));

        var result = service.convert("USD", "EUR", 0.0);

        assertThat(result.convertedAmount()).isEqualTo(0.0);
    }

    @Test
    void ratesForReturnsAllRatesForTheGivenCurrency() {
        server.expect(requestTo(BASE_URL + "/latest?amount=1&from=USD"))
                .andRespond(withSuccess("{\"amount\":1.0,\"base\":\"USD\",\"date\":\"2026-07-29\",\"rates\":{\"EUR\":0.9,\"JPY\":149.5}}", MediaType.APPLICATION_JSON));

        var result = service.ratesFor("usd");

        assertThat(result.date()).isEqualTo("2026-07-29");
        assertThat(result.rates()).containsEntry("EUR", 0.9).containsEntry("JPY", 149.5);
        server.verify();
    }

    @Test
    void ratesForThrowsBadRequestForUnknownCurrency() {
        server.expect(requestTo(BASE_URL + "/latest?amount=1&from=XXX"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).body("not found").contentType(MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> service.ratesFor("XXX"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);
        server.verify();
    }
}
