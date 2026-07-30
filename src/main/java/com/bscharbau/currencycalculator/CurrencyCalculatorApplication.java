package com.bscharbau.currencycalculator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class CurrencyCalculatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CurrencyCalculatorApplication.class, args);
    }

    @RestController
    class HelloController {
        @GetMapping("/hello")
        String hello() { return "Hello from Spring Boot"; }
    }
}
