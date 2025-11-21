package com.project.unifiedMarketingGateway.connector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
public class TelegramHttpConnector implements ConnectorInterface{

    @Value("${telegram.bot.token}")
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
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(body -> log.debug("Telegram {} success (resp length={})", method, body == null ? 0 : body.length()))
                .doOnError(e -> log.warn("Telegram {} error for payload {} : {}", method, payload, e.toString()));
    }

    private String apiPath(String method) {
        return "/bot" + botToken + "/" + method;
    }
}
