package com.project.unifiedMarketingGateway.responseStore;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Slf4j
public class WhatsappResponseStore {

    // in-memory store for demo. Replace with DB persistence if desired.
    private final ConcurrentMap<String, String> responses = new ConcurrentHashMap<>();

    public void storeResponse(String recipientId, String responseBody) {
        if (recipientId == null) recipientId = "unknown";
        responses.put(recipientId, responseBody == null ? "" : responseBody);
        log.info("Stored response for {} (len={})", recipientId, responseBody == null ? 0 : responseBody.length());
    }

    //TODO: use to provide response status for chat-id
    //TODO: persist the responses in bigQuery or some other dataLake
}

