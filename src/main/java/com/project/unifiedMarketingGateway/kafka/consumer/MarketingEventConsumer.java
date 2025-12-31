package com.project.unifiedMarketingGateway.kafka.consumer;

import com.project.unifiedMarketingGateway.kafka.model.MarketingCampaignRequest;
import com.project.unifiedMarketingGateway.processor.NotificationProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import static com.project.unifiedMarketingGateway.constants.KafkaTopicConstants.MARKETING_REQUESTS;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketingEventConsumer {

    private final NotificationProcessor notificationProcessor;

    @KafkaListener(
            topics = MARKETING_REQUESTS,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(MarketingCampaignRequest event, Acknowledgment ack) {

        log.info("Consumed event {}", event.getRequestId());
        notificationProcessor.processKafkaEvent(event);
        ack.acknowledge();
    }
}
