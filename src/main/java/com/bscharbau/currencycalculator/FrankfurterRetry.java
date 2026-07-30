package com.bscharbau.currencycalculator;

import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

// Retries transient failures (timeouts, connection errors, 5xx); 4xx errors like an unknown currency code are not retried.
final class FrankfurterRetry {

    private static final int MAX_ATTEMPTS = 3;
    private static final Duration INITIAL_BACKOFF = Duration.ofMillis(300);

    private FrankfurterRetry() {
    }

    static <T> T execute(Supplier<T> call) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return call.get();
            } catch (ResourceAccessException | HttpServerErrorException e) {
                if (attempt == MAX_ATTEMPTS) {
                    throw e;
                }
                sleep(INITIAL_BACKOFF.multipliedBy(attempt));
            }
        }
        throw new IllegalStateException("Unreachable");
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying Frankfurter API call", e);
        }
    }
}
