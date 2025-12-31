package com.project.unifiedMarketingGateway.kafka.producer;

import com.project.unifiedMarketingGateway.kafka.model.MarketingCampaignRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static com.project.unifiedMarketingGateway.constants.KafkaTopicConstants.MARKETING_REQUESTS;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketingEventPublisher {

    private final KafkaTemplate<String, MarketingCampaignRequest> kafkaTemplate;

    public Mono<String> publish(MarketingCampaignRequest event) {
        return Mono.fromFuture(
                kafkaTemplate.send(MARKETING_REQUESTS, event.getRequestId(), event)
        ).map(result -> {
            log.info("Kafka published requestId={}", event.getRequestId());
            return event.getRequestId();
        }).doOnError(ex ->
                log.error("Kafka publish failed requestId={}", event.getRequestId(), ex)
        );
    }
}
