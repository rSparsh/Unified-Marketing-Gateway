package com.project.unifiedMarketingGateway.messageStore;

import com.project.unifiedMarketingGateway.enums.WhatsappMessageStatus;
import com.project.unifiedMarketingGateway.models.WhatsappMessage;

import java.util.Optional;

public interface WhatsappMessageStore {
    void saveOutbound(WhatsappMessage message);

    void updateStatus(String waMessageId,
            String recipient,
            WhatsappMessageStatus status,
            String errorCode,
            String errorDetails);

    Optional<WhatsappMessage> findByWaMessageId(String waMessageId);
}
