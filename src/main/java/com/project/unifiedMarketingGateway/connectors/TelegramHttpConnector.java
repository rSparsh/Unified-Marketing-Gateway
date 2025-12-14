package com.project.unifiedMarketingGateway.connectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
public class TelegramHttpConnector implements ConnectorInterface{

    @Value("${telegram.bot.token:PLEASE_SET_SECRETS}")
    private String botToken;

    private final WebClient webClient;

    public TelegramHttpConnector(@Value("${telegram.baseUrl}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public Mono<String> sendMarketingRequest(String method, Map<String, Object> payload) {
        return webClient.post()
                .uri(apiPath(method))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class);
    }

    private String apiPath(String method) {
        return "/bot" + botToken + "/" + method;
    }
}
