package com.project.unifiedMarketingGateway.messageStore;

import com.project.unifiedMarketingGateway.enums.WhatsappMessageStatus;
import com.project.unifiedMarketingGateway.models.WhatsappMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Slf4j
public class InMemoryWhatsappMessageStore implements WhatsappMessageStore{

    private final ConcurrentMap<String, WhatsappMessage> store = new ConcurrentHashMap<>();

    @Override
    public void saveOutbound(WhatsappMessage message) {
        if (message.getWaMessageId() == null) {
            log.warn("saveOutbound called with null waMessageId, ignoring");
            return;
        }
        store.put(message.getWaMessageId(), message);
    }

    @Override
    public void updateStatus(String waMessageId,
            String recipient,
            WhatsappMessageStatus status,
            String errorCode,
            String errorDetails) {
        if (waMessageId == null) return;

        store.compute(waMessageId, (id, existing) -> {
            if (existing == null) {
                // message created only via webhook, create a minimal record
                existing = WhatsappMessage.builder()
                        .waMessageId(waMessageId)
                        .recipient(recipient)
                        .createdAtEpochMillis(System.currentTimeMillis())
                        .build();
            }
            existing.setStatus(status);
            existing.setLastUpdatedEpochMillis(System.currentTimeMillis());
            existing.setErrorCode(errorCode);
            existing.setErrorDetails(errorDetails);
            return existing;
        });
    }

    @Override
    public Optional<WhatsappMessage> findByWaMessageId(String waMessageId) {
        return Optional.ofNullable(store.get(waMessageId));
    }
}