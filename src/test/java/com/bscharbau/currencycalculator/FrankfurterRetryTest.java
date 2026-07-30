package com.bscharbau.currencycalculator;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FrankfurterRetryTest {

    @Test
    void returnsResultWithoutRetryingOnSuccess() {
        AtomicInteger calls = new AtomicInteger();

        String result = FrankfurterRetry.execute(() -> {
            calls.incrementAndGet();
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void retriesOnResourceAccessExceptionAndEventuallySucceeds() {
        AtomicInteger calls = new AtomicInteger();

        String result = FrankfurterRetry.execute(() -> {
            if (calls.incrementAndGet() < 3) {
                throw new ResourceAccessException("simulated timeout");
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void retriesOnHttpServerErrorExceptionAndEventuallySucceeds() {
        AtomicInteger calls = new AtomicInteger();

        String result = FrankfurterRetry.execute(() -> {
            if (calls.incrementAndGet() < 2) {
                throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void givesUpAfterMaxAttemptsAndRethrowsLastFailure() {
        AtomicInteger calls = new AtomicInteger();
        Supplier<String> alwaysFails = () -> {
            calls.incrementAndGet();
            throw new ResourceAccessException("still down");
        };

        assertThatThrownBy(() -> FrankfurterRetry.execute(alwaysFails))
                .isInstanceOf(ResourceAccessException.class)
                .hasMessage("still down");
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void doesNotRetryClientErrors() {
        AtomicInteger calls = new AtomicInteger();
        Supplier<String> badRequest = () -> {
            calls.incrementAndGet();
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST);
        };

        assertThatThrownBy(() -> FrankfurterRetry.execute(badRequest))
                .isInstanceOf(HttpClientErrorException.class);
        assertThat(calls.get()).isEqualTo(1);
    }
}
