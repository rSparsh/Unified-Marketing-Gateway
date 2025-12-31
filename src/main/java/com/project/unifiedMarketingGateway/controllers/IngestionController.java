package com.project.unifiedMarketingGateway.controllers;

import com.project.unifiedMarketingGateway.kafka.model.MarketingCampaignRequest;
import com.project.unifiedMarketingGateway.kafka.model.MarketingCampaignResponse;
import com.project.unifiedMarketingGateway.kafka.producer.MarketingEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/ingestMarketingEvent")
@RequiredArgsConstructor
public class IngestionController {

    private final MarketingEventPublisher publisher;

    @PostMapping
    public Mono<ResponseEntity<MarketingCampaignResponse>> publish(@RequestBody MarketingCampaignRequest event) {
        event.setRequestId(UUID.randomUUID().toString());
        event.setCreatedAtEpochMillis(Instant.now().toEpochMilli());

        return publisher.publish(event)
                .map(id -> ResponseEntity.ok(
                        MarketingCampaignResponse.builder()
                                .isQueued(true)
                                .requestId(id)
                                .build()))
                .onErrorResume(ex ->
                        Mono.just(
                                ResponseEntity.status(500)
                                        .body(MarketingCampaignResponse.builder()
                                                .isQueued(false).build())
                        )
                );
    }
}
