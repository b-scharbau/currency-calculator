package com.bscharbau.currencycalculator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CurrencyConversionController.class)
class CurrencyConversionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExchangeRateService exchangeRateService;

    @Test
    void convertReturnsConversionResult() throws Exception {
        given(exchangeRateService.convert("USD", "EUR", 10.0)).willReturn(
                new CurrencyConversionController.ConversionResult("USD", "EUR", 10.0, 8.79, 0.879, "2026-07-29"));

        mockMvc.perform(get("/convert").param("amount", "10").param("from", "USD").param("to", "EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("USD"))
                .andExpect(jsonPath("$.to").value("EUR"))
                .andExpect(jsonPath("$.amount").value(10.0))
                .andExpect(jsonPath("$.convertedAmount").value(8.79))
                .andExpect(jsonPath("$.rate").value(0.879))
                .andExpect(jsonPath("$.date").value("2026-07-29"));
    }

    @Test
    void convertPropagatesStatusFromServiceForUnknownCurrency() throws Exception {
        given(exchangeRateService.convert("XXX", "EUR", 1.0))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown currency code: XXX"));

        mockMvc.perform(get("/convert").param("amount", "1").param("from", "XXX").param("to", "EUR"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void convertRequiresAllQueryParams() throws Exception {
        mockMvc.perform(get("/convert").param("from", "USD").param("to", "EUR"))
                .andExpect(status().isBadRequest());
    }
}
