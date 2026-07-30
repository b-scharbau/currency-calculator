package com.bscharbau.currencycalculator;

import java.util.List;
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
    void currencyReturnsOneOfTheAvailableCurrencies() throws Exception {
        given(currencyService.fetchCurrencies()).willReturn(List.of(
                new CurrencyController.Currency("JPY", "Japanese Yen")
        ));

        mockMvc.perform(get("/currency"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("JPY"))
                .andExpect(jsonPath("$.name").value("Japanese Yen"));
    }
}
