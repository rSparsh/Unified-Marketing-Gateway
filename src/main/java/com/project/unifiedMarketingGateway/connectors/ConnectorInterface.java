package com.project.unifiedMarketingGateway.connectors;

import reactor.core.publisher.Mono;

import java.util.Map;

public interface ConnectorInterface {

    public Mono<String> sendMarketingRequest(String method, Map<String, Object> payload) ;
}
