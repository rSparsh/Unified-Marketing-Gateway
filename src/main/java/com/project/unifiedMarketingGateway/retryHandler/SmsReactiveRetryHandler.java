package com.project.unifiedMarketingGateway.retryHandler;

import com.project.unifiedMarketingGateway.metrics.MetricsService;
import com.twilio.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.function.Supplier;

import static com.project.unifiedMarketingGateway.enums.ClientType.SMS;

@Slf4j
@Service
public class SmsReactiveRetryHandler implements ReactiveRetryHandlerInterface{

    @Autowired
    MetricsService metricsService;

    @Value("${sms.retry.maxRetryCount:4}")
    private int maxRetries;

    @Value("${sms.retry.initialBackoff:1}")
    private int initialBackoffValue;

    private final Duration initialBackoff = Duration.ofSeconds(initialBackoffValue);

    @Override
    public <T> Mono<T> withRetry(Supplier<Mono<T>> publisherSupplier) {

        return Mono.defer(publisherSupplier)
                .retryWhen(
                        Retry.backoff(maxRetries, initialBackoff)
                                .filter(this::isRetryable)
                                .doBeforeRetry(retrySignal ->
                                        {
                                            metricsService.incrementRetry(SMS.getValue());
                                            log.warn(
                                                    "Retrying SMS attempt {} due to: {}",
                                                    retrySignal.totalRetries() + 1,
                                                    retrySignal.failure().toString()
                                            );
                                        }
                                )
                                .onRetryExhaustedThrow((spec, signal) -> signal.failure())
                );
    }

    private boolean isRetryable(Throwable throwable) {

        // Network / unexpected runtime issues → retry
        if (!(throwable instanceof ApiException)) {
            return true;
        }

        ApiException apiEx = (ApiException) throwable;
        int status = apiEx.getStatusCode();

        // Retry on Twilio server errors or throttling
        return status >= 500 || status == 429;
    }
}

