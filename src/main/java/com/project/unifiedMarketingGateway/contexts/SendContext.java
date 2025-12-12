package com.project.unifiedMarketingGateway.contexts;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;

@Data
@Builder
public class SendContext {

    private final String channel;
    private final String method;
    private final String recipient;
    private final String requestId;

    private long startNano;

    public void markStart() {
        this.startNano = System.nanoTime();
    }

    public Duration elapsed() {
        if (startNano == 0) return Duration.ZERO;
        return Duration.ofNanos(System.nanoTime() - startNano);
    }
}
