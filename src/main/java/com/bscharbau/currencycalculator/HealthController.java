package com.bscharbau.currencycalculator;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    HealthStatus health() {
        return new HealthStatus("UP");
    }

    record HealthStatus(String status) {
    }
}
