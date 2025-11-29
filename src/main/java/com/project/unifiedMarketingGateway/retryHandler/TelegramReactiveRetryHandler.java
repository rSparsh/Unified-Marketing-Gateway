package com.project.unifiedMarketingGateway.retryHandler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Component
@Slf4j
public class TelegramReactiveRetryHandler implements ReactiveRetryHandlerInterface {

    @Value("${telegram.retry.maxRetryCount:4}")
    private int maxRetries;

    @Value("${telegram.retry.initialBackoff:1}")
    private int initialBackoffValue;

    @Value("${telegram.retry.maxBackoff:5}")
    private int maxBackoffValue;

    private final Duration initialBackoff = Duration.ofSeconds(initialBackoffValue);
    private final Duration maxBackoff = Duration.ofSeconds(maxBackoffValue);

    /**
     * Generic retry wrapper. Supply a fresh Supplier<Mono<T>> for each invocation
     * (important: Mono must be created fresh per subscription).
     *
     * @param publisherSupplier supplier that returns a Mono when invoked
     * @param <T> result type
     * @return Mono<T> that retries according to policy
     */
    @Override
    public <T> Mono<T> withRetry(Supplier<Mono<T>> publisherSupplier) {
        return Mono.defer(publisherSupplier)
                .retryWhen(Retry.from(companion -> companion.flatMap(signal -> {
                    Throwable failure = signal.failure();
                    int attempt = (int) signal.totalRetriesInARow() + 1;

                    if (!isRetryable(failure)) {
                        return Mono.error(failure);
                    }

                    if (attempt > maxRetries) {
                        log.warn("Exhausted retries after {} attempts; failing", attempt - 1);
                        return Mono.error(failure);
                    }

                    Duration retryAfter = extractRetryAfter(failure);
                    Duration delay = retryAfter != null ? retryAfter : computeBackoff(attempt);

                    log.info("Retry attempt={} will retry after {} ms due to: {}", attempt, delay.toMillis(), shortError(failure));
                    return Mono.delay(delay);
                })));
    }

    private Duration computeBackoff(int attempt) {
        long base = initialBackoff.toMillis() * (1L << (attempt - 1));
        long capped = Math.min(base, maxBackoff.toMillis());
        // add +/- 20% jitter
        double jitterFactor = 0.2;
        long jitter = (long) (ThreadLocalRandom.current().nextDouble(-jitterFactor, jitterFactor) * capped);
        return Duration.ofMillis(Math.max(100L, capped + jitter));
    }

    /**
     * Retry is only attempted for temporary errors,
     * but not for permanent errors like 4xx.
     * WebClientRequestException --> networkErrors
     * 5xx --> serverErrors
     * 429 --> rateLimit
     */
    private boolean isRetryable(Throwable t) {
        if (t == null) return false;
        if (t instanceof WebClientRequestException) return true;
        if (t instanceof WebClientResponseException wre) {
            int status = wre.getRawStatusCode();
            if (status >= 500 && status < 600) return true;
            if (status == 429) return true;
            return false;
        }
        return false;
    }

    private Duration extractRetryAfter(Throwable t) {
        if (!(t instanceof WebClientResponseException wre)) return null;
        String value = wre.getHeaders().getFirst("Retry-After");
        if (value == null) return null;
        try {
            long seconds = Long.parseLong(value.trim());
            return Duration.ofSeconds(seconds);
        } catch (NumberFormatException ex) {
            try {
                Instant date = DateTimeFormatter.RFC_1123_DATE_TIME.parse(value, Instant::from);
                long secs = Math.max(0, date.getEpochSecond() - Instant.now().getEpochSecond());
                return Duration.ofSeconds(secs);
            } catch (Exception ignore) {
                return null;
            }
        }
    }

    private String shortError(Throwable t) {
        if (t == null) return "none";
        if (t instanceof WebClientResponseException wre) {
            String body = "";
            try { body = wre.getResponseBodyAsString(); } catch (Exception ignored) {}
            return "status=" + wre.getRawStatusCode() + " body=" + body;
        }
        return t.getClass().getSimpleName() + ":" + t.getMessage();
    }
}

