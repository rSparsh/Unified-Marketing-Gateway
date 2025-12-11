package com.project.unifiedMarketingGateway.retryHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Service
@Slf4j
public class WhatsappReactiveRetryHandler implements ReactiveRetryHandlerInterface {

    private static final Set<Integer> RETRYABLE_WHATSAPP_ERROR_CODES = Set.of(
            1,       // API Unknown (can be temporary) – treat as retryable
            2,       // API Service – temporary downtime/overload
            4,       // API Too Many Calls – rate-limited
            17,      // User Too Many Calls – rate-limited
            341,     // Application limit reached – throttling
            80007,   // Rate limit issues (WABA-level limits)
            130429,  // Rate limit hit – Cloud API throughput reached
            131000,  // Something went wrong – unknown server error
            131016,  // Service unavailable / internal error
            131056   // Pair rate limit hit – too many msgs to same recipient
    );

    @Autowired
    ObjectMapper objectMapper;

    @Value("${whatsapp.retry.maxRetryCount:4}")
    private int maxRetries;

    @Value("${whatsapp.retry.initialBackoff:1}")
    private int initialBackoffValue;

    @Value("${whatsapp.retry.maxBackoff:5}")
    private int maxBackoffValue;

    private final Duration initialBackoff = Duration.ofSeconds(initialBackoffValue);
    private final Duration maxBackoff = Duration.ofSeconds(maxBackoffValue);
    private final Scheduler scheduler = Schedulers.boundedElastic();

    @Override
    public <T> Mono<T> withRetry(Supplier<Mono<T>> supplier) {
        return Mono.defer(supplier)
                .retryWhen(Retry.from(companion -> companion.flatMap(signal -> {
                    Throwable failure = signal.failure();
                    int attempt = (int) signal.totalRetriesInARow() + 1; // 1-based attempt count

                    if (!isRetryable(failure)) {
                        log.debug("[WA-Retry] Non-retryable error, giving up immediately: {}", shortError(failure));
                        return Mono.error(failure);
                    }

                    if (attempt > maxRetries) {
                        log.warn("[WA-Retry] Exhausted retries after {} attempts. Last error: {}",
                                attempt - 1, shortError(failure));
                        return Mono.error(failure);
                    }

                    Duration retryAfter = extractRetryAfter(failure);
                    Duration delay = (retryAfter != null) ? retryAfter : computeBackoff(attempt);

                    log.info("[WA-Retry] Retrying attempt={} after {} ms due to {}",
                            attempt, delay.toMillis(), shortError(failure));

                    return Mono.delay(delay, scheduler);
                })));
    }

    /** Decide if an error is worth retrying for WhatsApp Cloud API. */
    private boolean isRetryable(Throwable t) {
        if (t == null) {
            return false;
        }

        // Network / I/O problems – always retryable
        if (t instanceof WebClientRequestException) {
            return true;
        }

        if (t instanceof WebClientResponseException ex) {
            int status = ex.getRawStatusCode();

            // Hard retry on these HTTP statuses
            if (status == 408) { // Request Timeout
                return true;
            }
            if (status == 429) { // Too Many Requests – explicit rate-limit
                return true;
            }
            if (status >= 500 && status < 600) { // Server errors
                return true;
            }

            // For 4xx, inspect WhatsApp / Graph error.code
            Integer waCode = extractWhatsAppErrorCode(ex);
            if (waCode != null) {
                if (RETRYABLE_WHATSAPP_ERROR_CODES.contains(waCode)) {
                    log.debug("[WA-Retry] Treating error.code={} as retryable", waCode);
                    return true;
                } else {
                    log.debug("[WA-Retry] WhatsApp error.code={} is not retryable", waCode);
                }
            }

            // Default: non-retryable client error
            return false;
        }

        // Everything else: non-retryable
        return false;
    }

    /**
     * Try to respect Retry-After when present.
     * (This WhatsApp/Graph error denotes 429/throttling).
     */
    private Duration extractRetryAfter(Throwable t) {
        if (!(t instanceof WebClientResponseException ex)) {
            return null;
        }

        String retryAfter = ex.getHeaders().getFirst("Retry-After");
        if (retryAfter == null) {
            return null;
        }

        try {
            // Case 1: integer seconds
            long secs = Long.parseLong(retryAfter.trim());
            return Duration.ofSeconds(Math.max(0, secs));
        } catch (NumberFormatException ignore) {
            // Case 2: HTTP-date
            try {
                Instant ts = DateTimeFormatter.RFC_1123_DATE_TIME.parse(retryAfter, Instant::from);
                long secs = Math.max(0, ts.getEpochSecond() - Instant.now().getEpochSecond());
                return Duration.ofSeconds(secs);
            } catch (Exception e) {
                log.debug("[WA-Retry] Invalid Retry-After value '{}': {}", retryAfter, e.toString());
                return null;
            }
        }
    }

    /**
     * Standard exponential backoff with jitter, capped at maxBackoff.
     */
    private Duration computeBackoff(int attempt) {
        long baseMillis = initialBackoff.toMillis() * (1L << (attempt - 1)); // 1,2,4,8...
        long capped = Math.min(baseMillis, maxBackoff.toMillis());
        double jitterFactor = 0.2; // +/-20%
        long jitter = (long) (ThreadLocalRandom.current().nextDouble(-jitterFactor, jitterFactor) * capped);
        long finalMillis = Math.max(1L, capped + jitter);
        return Duration.ofMillis(finalMillis);
    }

    /**
     * Parse WhatsApp/Graph error.code from the JSON body:
     * {
     *   "error": {
     *     "message": "...",
     *     "type": "...",
     *     "code": ...,
     *     "error_subcode": ...,
     *     ...
     *   }
     * }
     */
    private Integer extractWhatsAppErrorCode(WebClientResponseException ex) {
        try {
            String body = ex.getResponseBodyAsString();
            if (body == null || body.isBlank()) {
                return null;
            }
            JsonNode root = objectMapper.readTree(body);
            JsonNode errorNode = root.get("error");
            if (errorNode == null) {
                return null;
            }
            JsonNode codeNode = errorNode.get("code");
            if (codeNode == null || !codeNode.isNumber()) {
                return null;
            }
            return codeNode.intValue();
        } catch (Exception e) {
            log.debug("[WA-Retry] Failed to parse WhatsApp error.code from body: {}", e.toString());
            return null;
        }
    }

    private String shortError(Throwable t) {
        if (t == null) return "null";
        if (t instanceof WebClientResponseException ex) {
            Integer code = extractWhatsAppErrorCode(ex);
            String body;
            try {
                body = ex.getResponseBodyAsString();
            } catch (Exception e) {
                body = "<unavailable>";
            }
            return "status=" + ex.getRawStatusCode() +
                    (code != null ? ", waCode=" + code : "") +
                    ", body=" + body;
        }
        return t.getClass().getSimpleName() + ": " + t.getMessage();
    }
}