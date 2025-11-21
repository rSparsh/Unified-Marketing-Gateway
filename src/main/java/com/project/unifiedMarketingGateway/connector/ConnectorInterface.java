package com.project.unifiedMarketingGateway.connector;

import reactor.core.publisher.Mono;

import java.util.Map;

public interface ConnectorInterface {

    public Mono<String> sendMarketingRequest(String method, Map<String, Object> payload) ;
}
