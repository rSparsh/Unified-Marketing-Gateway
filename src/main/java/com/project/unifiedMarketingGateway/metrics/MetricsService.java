package com.project.unifiedMarketingGateway.metrics;

import io.micrometer.core.instrument.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class MetricsService {

    @Getter
    private final MeterRegistry registry;

    public static final String TAG_CHANNEL = "channel";
    public static final String TAG_MEDIA = "media";
    public static final String TAG_STATUS = "status";

    public static final String METRIC_SENDS_TOTAL = "umg.sends.total";
    public static final String METRIC_SENDS_SUCCESS = "umg.sends.success";
    public static final String METRIC_SENDS_FAILED = "umg.sends.failed";
    public static final String METRIC_SEND_RETRIES = "umg.sends.retries";
    public static final String METRIC_HTTP_LATENCY = "umg.http.latency";
    public static final String METRIC_WEBHOOK_EVENTS = "umg.webhook.events";
    public static final String METRIC_IN_FLIGHT = "umg.inflight.requests";

    private final Map<String, AtomicInteger> inFlightByChannel = new ConcurrentHashMap<>();

    @Getter(lazy = true)
    private final Counter sendsTotalCounter = Counter.builder(METRIC_SENDS_TOTAL)
            .description("Total sends attempted")
            .register(registry);

    @Getter(lazy = true)
    private final Counter sendsRetriesCounter = Counter.builder(METRIC_SEND_RETRIES)
            .description("Retry attempts")
            .register(registry);

    public void incrementSendAttempt(String channel, String media) {
        registry.counter(METRIC_SENDS_TOTAL, TAG_CHANNEL, safe(channel), TAG_MEDIA, safe(media)).increment();
    }

    public void incrementSendSuccess(String channel, String media) {
        registry.counter(METRIC_SENDS_SUCCESS, TAG_CHANNEL, safe(channel), TAG_MEDIA, safe(media)).increment();
    }

    public void incrementSendFailure(String channel, String media, String reason) {
        String reasonTag = (reason == null || reason.isBlank()) ? "unknown" : sanitizeReason(reason);
        registry.counter(METRIC_SENDS_FAILED, TAG_CHANNEL, safe(channel), TAG_MEDIA, safe(media), "reason", reasonTag).increment();
    }

    public void incrementRetry(String channel) {
        registry.counter(METRIC_SEND_RETRIES, TAG_CHANNEL, safe(channel)).increment();
    }

    public void recordHttpLatency(String channel, String media, Duration duration) {
        Timer.builder(METRIC_HTTP_LATENCY)
                .description("HTTP call latency to provider")
                .tags(TAG_CHANNEL, safe(channel), TAG_MEDIA, safe(media))
                .publishPercentiles(0.5, 0.95) // optional
                .publishPercentileHistogram(false)
                .register(registry)
                .record(duration);
    }

    public int incrementInFlight(String channel) {
        AtomicInteger g = inFlightByChannel.computeIfAbsent(safe(channel), k -> {
            AtomicInteger a = new AtomicInteger(0);
            Gauge.builder(METRIC_IN_FLIGHT, a, AtomicInteger::get)
                    .description("In-flight requests per channel")
                    .tag(TAG_CHANNEL, k)
                    .baseUnit("requests")
                    .register(registry);
            return a;
        });
        return g.incrementAndGet();
    }

    public int decrementInFlight(String channel) {
        AtomicInteger g = inFlightByChannel.get(safe(channel));
        if (g == null) return 0;
        return Math.max(0, g.decrementAndGet());
    }

    public void incrementWebhookEvent(String eventType) {
        registry.counter(METRIC_WEBHOOK_EVENTS, "event", safe(eventType)).increment();
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "unknown" : s.toLowerCase();
    }

    private String sanitizeReason(String reason) {
        String r = reason.toLowerCase();
        if (r.contains("timeout")) return "timeout";
        if (r.contains("429") || r.contains("rate")) return "rate_limit";
        if (r.contains("5") || r.contains("server")) return "server_error";
        if (r.contains("auth") || r.contains("unauthorized") || r.contains("403") || r.contains("401")) return "auth";
        return "other";
    }
}
