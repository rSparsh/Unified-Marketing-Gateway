package com.project.unifiedMarketingGateway.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.unifiedMarketingGateway.enums.WhatsappMessageStatus;
import com.project.unifiedMarketingGateway.metrics.MetricsService;
import com.project.unifiedMarketingGateway.store.messageStore.WhatsappMessageStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class WhatsappWebhookService {

    @Autowired ObjectMapper objectMapper;
    @Autowired WhatsappMessageStore messageStore;
    @Autowired
    MetricsService metricsService;

    public void processWebhookPayload(Map<String, Object> rawPayload) {
        JsonNode root = objectMapper.valueToTree(rawPayload);

        JsonNode entryArr = root.get("entry");
        if (entryArr == null || !entryArr.isArray()) {
            log.debug("WA webhook: no entry array");
            return;
        }

        for (JsonNode entry : entryArr) {
            JsonNode changes = entry.get("changes");
            if (changes == null || !changes.isArray()) continue;

            for (JsonNode change : changes) {
                JsonNode value = change.get("value");
                if (value == null) continue;

                JsonNode statuses = value.get("statuses");
                if (statuses != null && statuses.isArray()) {
                    for (JsonNode statusNode : statuses) {
                        handleStatus(statusNode);
                    }
                }
            }
        }
    }

    private void handleStatus(JsonNode statusNode) {
        String messageId   = getText(statusNode, "id");
        String statusStr   = getText(statusNode, "status");
        String recipientId = getText(statusNode, "recipient_id");

        metricsService.incrementWebhookEvent("status_" + statusStr);

        String errorCode = null;
        String errorDetails = null;
        JsonNode errors = statusNode.get("errors");
        if (errors != null && errors.isArray() && !errors.isEmpty()) {
            JsonNode err = errors.get(0);
            errorCode = getText(err, "code");
            errorDetails = getText(err, "details");
        }

        WhatsappMessageStatus mappedStatus = mapStatus(statusStr);

        log.info("WA webhook status: msgId={} recipient={} status={} errCode={} errDetails={}",
                messageId, recipientId, statusStr, errorCode, errorDetails);

        messageStore.updateStatus(messageId, recipientId, mappedStatus, errorCode, errorDetails);
    }

    private String getText(JsonNode node, String fieldName) {
        if (node == null) return null;
        JsonNode v = node.get(fieldName);
        return (v != null && !v.isNull()) ? v.asText() : null;
    }

    private WhatsappMessageStatus mapStatus(String s) {
        if (s == null) return WhatsappMessageStatus.SENT;
        return switch (s) {
            case "sent"      -> WhatsappMessageStatus.SENT;
            case "delivered" -> WhatsappMessageStatus.DELIVERED;
            case "read"      -> WhatsappMessageStatus.READ;
            case "failed"    -> WhatsappMessageStatus.FAILED;
            default          -> WhatsappMessageStatus.SENT;
        };
    }
}
