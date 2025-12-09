package com.project.unifiedMarketingGateway.connectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
public class WhatsappHttpConnector implements ConnectorInterface {

    @Value("${whatsapp.accessToken}")
    private String accessToken;

    private final WebClient webClient;

    public WhatsappHttpConnector(@Value("${whatsapp.baseUrl:https://graph.facebook.com}") String baseUrl,
            @Value("${whatsapp.api-version:v20.0}") String apiVersion,
            @Value("${whatsapp.phone-number-id}") String phoneNumberId) {

        // final base url becomes: https://graph.facebook.com/v20.0/{phone_number_id}
        String fullBaseUrl = baseUrl + "/" + apiVersion + "/" + phoneNumberId;

        this.webClient = WebClient.builder()
                .baseUrl(fullBaseUrl)
                .build();
    }

    @Override
    public Mono<String> sendMarketingRequest(String method, Map<String, Object> payload) {
        String path = (method == null || method.isBlank()) ? "/messages" :
                (method.startsWith("/") ? method : "/" + method);

        return webClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class);
    }
}
