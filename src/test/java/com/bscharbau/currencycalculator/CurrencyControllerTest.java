package com.bscharbau.currencycalculator;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CurrencyController.class)
class CurrencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrencyService currencyService;

    @MockitoBean
    private ExchangeRateService exchangeRateService;

    @Test
    void currenciesReturnsFullList() throws Exception {
        given(currencyService.fetchCurrencies()).willReturn(List.of(
                new CurrencyController.Currency("EUR", "Euro"),
                new CurrencyController.Currency("USD", "US Dollar")
        ));

        mockMvc.perform(get("/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].code").value("EUR"))
                .andExpect(jsonPath("$[0].name").value("Euro"))
                .andExpect(jsonPath("$[1].code").value("USD"));
    }

    @Test
    void currenciesReturnsEmptyListWhenNoneStored() throws Exception {
        given(currencyService.fetchCurrencies()).willReturn(List.of());

        mockMvc.perform(get("/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void currencyWithoutCodeReturnsRandomCurrencyWithRates() throws Exception {
        given(currencyService.fetchCurrencies()).willReturn(List.of(
                new CurrencyController.Currency("JPY", "Japanese Yen")
        ));
        given(exchangeRateService.ratesFor("JPY")).willReturn(
                new ExchangeRateService.AllRatesResult("2026-07-29", Map.of("EUR", 0.006, "USD", 0.0067)));

        mockMvc.perform(get("/currency"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("JPY"))
                .andExpect(jsonPath("$.name").value("Japanese Yen"))
                .andExpect(jsonPath("$.date").value("2026-07-29"))
                .andExpect(jsonPath("$.rates", hasSize(2)))
                .andExpect(jsonPath("$.rates[0].to").value("EUR"))
                .andExpect(jsonPath("$.rates[0].rate").value(0.006))
                .andExpect(jsonPath("$.rates[1].to").value("USD"))
                .andExpect(jsonPath("$.rates[1].rate").value(0.0067));
    }

    @Test
    void currencyWithCodeReturnsThatSpecificCurrency() throws Exception {
        given(currencyService.fetchCurrencies()).willReturn(List.of(
                new CurrencyController.Currency("JPY", "Japanese Yen"),
                new CurrencyController.Currency("USD", "US Dollar")
        ));
        given(exchangeRateService.ratesFor("USD")).willReturn(
                new ExchangeRateService.AllRatesResult("2026-07-29", Map.of("JPY", 149.5)));

        mockMvc.perform(get("/currency").param("code", "usd"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("USD"))
                .andExpect(jsonPath("$.name").value("US Dollar"))
                .andExpect(jsonPath("$.rates", hasSize(1)))
                .andExpect(jsonPath("$.rates[0].to").value("JPY"))
                .andExpect(jsonPath("$.rates[0].rate").value(149.5));
    }

    @Test
    void currencyWithUnknownCodeReturnsBadRequest() throws Exception {
        given(currencyService.fetchCurrencies()).willReturn(List.of(
                new CurrencyController.Currency("JPY", "Japanese Yen")
        ));

        mockMvc.perform(get("/currency").param("code", "XXX"))
                .andExpect(status().isBadRequest());
    }
}
