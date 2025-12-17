package com.project.unifiedMarketingGateway.processor;

import com.project.unifiedMarketingGateway.configs.FallbackPolicy;
import com.project.unifiedMarketingGateway.entity.DeliveryStateEntity;
import com.project.unifiedMarketingGateway.processor.sms.SmsRequestProcessor;
import com.project.unifiedMarketingGateway.repository.DeliveryStateRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FallbackRouter {

    @Autowired
    DeliveryStateRepository deliveryRepo;
    @Autowired FallbackPolicy policy;
    @Autowired SmsRequestProcessor smsProcessor;

    @Value("${whatsapp.fallbackRouting.isEnabled:false}")
    boolean isFallBackRoutingEnabled;

    @Transactional
    public void attemptFallback(DeliveryStateEntity state) {

        if (isFallBackRoutingEnabled && !policy.shouldFallback(state)) {
            return;
        }

        String fallbackChannel = policy.fallbackChannel(state.getChannel());

        log.warn(
                "Triggering fallback: requestId={} recipient={} {} → {}",
                state.getRequestId(),
                state.getRecipient(),
                state.getChannel(),
                fallbackChannel
        );

        // Mark fallback immediately to prevent races
        state.setFallbackTriggered(true);
        state.setFallbackChannel(fallbackChannel);
        deliveryRepo.save(state);

        // Dispatch fallback (async, fire-and-forget)
        smsProcessor.processFallback(
                state.getRequestId(),
                state.getRecipient()
        );
    }
}
