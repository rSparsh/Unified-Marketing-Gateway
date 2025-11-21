package com.project.unifiedMarketingGateway.responseStore;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Slf4j
public class TelegramResponseStore {

    // in-memory store for demo. Replace with DB persistence if desired.
    private final ConcurrentMap<String, String> responses = new ConcurrentHashMap<>();

    public void storeResponse(String recipientId, String responseBody) {
        if (recipientId == null) recipientId = "unknown";
        responses.put(recipientId, responseBody == null ? "" : responseBody);
        log.info("Stored response for {} (len={})", recipientId, responseBody == null ? 0 : responseBody.length());
    }

    public Optional<String> getResponse(String recipientId) {
        return Optional.ofNullable(responses.get(recipientId));
    }

    // for persistence in background (optional)
    @Async("telegramExecutor")
    public void persistResponseAsync(String recipientId, String responseBody) {
        // replace with actual persistence logic (DB call, audit log, etc.)
        log.info("Persisting response for {} in background", recipientId);
        // persistenceRepo.save(...);
    }
}

