package com.bscharbau.currencycalculator;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI currencyCalculatorOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Currency Calculator API")
                .description("Convert between currencies using live Frankfurter exchange rates.")
                .version("v1"));
    }
}
