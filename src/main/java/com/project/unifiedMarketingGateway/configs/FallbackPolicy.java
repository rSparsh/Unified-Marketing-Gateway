package com.project.unifiedMarketingGateway.configs;

import com.project.unifiedMarketingGateway.entity.DeliveryStateEntity;
import com.project.unifiedMarketingGateway.enums.DeliveryStatus;
import org.springframework.stereotype.Component;

import static com.project.unifiedMarketingGateway.enums.ClientType.SMS;
import static com.project.unifiedMarketingGateway.enums.ClientType.WHATSAPP;

@Component
public class FallbackPolicy {

    public boolean shouldFallback(DeliveryStateEntity state) {

        if (!WHATSAPP.getValue().equals(state.getChannel())) {
            return false;
        }

        if (Boolean.TRUE.equals(state.getFallbackTriggered())) {
            return false;
        }

        return state.getStatus() == DeliveryStatus.FAILED
                || state.getStatus() == DeliveryStatus.SENT;
    }

    public String fallbackChannel(String channel) {
        if (!WHATSAPP.getValue().equals(channel)) {
            return SMS.getValue();
        }
        throw new IllegalArgumentException("No fallback for channel " + channel);
    }
}

