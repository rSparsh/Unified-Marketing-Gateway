package com.project.unifiedMarketingGateway.retryHandler;

import reactor.core.publisher.Mono;

import java.util.function.Supplier;

public interface ReactiveRetryHandlerInterface {

    public <T> Mono<T> withRetry(Supplier<Mono<T>> publisherSupplier);
}
