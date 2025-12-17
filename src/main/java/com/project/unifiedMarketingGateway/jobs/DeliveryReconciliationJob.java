package com.project.unifiedMarketingGateway.jobs;

import com.project.unifiedMarketingGateway.entity.DeliveryStateEntity;
import com.project.unifiedMarketingGateway.enums.DeliveryStatus;
import com.project.unifiedMarketingGateway.enums.ReconciliationResult;
import com.project.unifiedMarketingGateway.metrics.MetricsService;
import com.project.unifiedMarketingGateway.processor.FallbackRouter;
import com.project.unifiedMarketingGateway.repository.DeliveryStateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class DeliveryReconciliationJob {

    @Autowired DeliveryStateRepository repository;
    @Autowired
    MetricsService metricsService;
    @Autowired
    FallbackRouter fallbackRouter;

    private final long queuedTimeoutMs;
    private final long sentTimeoutMs;
    private final long deliveredTimeoutMs;

    public DeliveryReconciliationJob(
            DeliveryStateRepository repository,
            @Value("${reconciliation.timeout.queued:5}") int queuedMinutes,
            @Value("${reconciliation.timeout.sent:30}") int sentMinutes,
            @Value("${reconciliation.timeout.delivered:1440}") int deliveredMinutes
    ) {
        this.repository = repository;
        this.queuedTimeoutMs = Duration.ofMinutes(queuedMinutes).toMillis();
        this.sentTimeoutMs = Duration.ofMinutes(sentMinutes).toMillis();
        this.deliveredTimeoutMs = Duration.ofMinutes(deliveredMinutes).toMillis();
    }

    @Scheduled(fixedDelay = 300000) // every 5 minutes
    public void reconcile() {
        long now = System.currentTimeMillis();

        reconcileQueued(now);
        reconcileSent(now);
        reconcileDelivered(now);
    }

    private void reconcileQueued(long now) {
        repository.findByStatus(DeliveryStatus.QUEUED)
                .stream()
                .filter(e -> isExpired(e, now, queuedTimeoutMs))
                .forEach(e ->
                        handle(e, ReconciliationResult.STUCK_QUEUED)
                );
    }

    private void reconcileSent(long now) {
        repository.findByStatus(DeliveryStatus.SENT)
                .stream()
                .filter(e -> isExpired(e, now, sentTimeoutMs))
                .forEach(e ->
                        handle(e, ReconciliationResult.STUCK_SENT)
                );
    }

    private void reconcileDelivered(long now) {
        repository.findByStatus(DeliveryStatus.DELIVERED)
                .stream()
                .filter(e -> isExpired(e, now, deliveredTimeoutMs))
                .forEach(e ->
                        handle(e, ReconciliationResult.STUCK_DELIVERED)
                );
    }

    private boolean isExpired(
            DeliveryStateEntity e,
            long now,
            long timeout
    ) {
        Long updatedAt = e.getUpdatedAtEpochMillis();
        return updatedAt != null && (now - updatedAt) > timeout;
    }

    private void handle(
            DeliveryStateEntity e,
            ReconciliationResult result
    ) {
        log.warn(
                "Reconciliation detected: result={} requestId={} channel={} recipient={} mediaType={} status={}",
                result,
                e.getRequestId(),
                e.getChannel(),
                e.getRecipient(),
                e.getMediaType(),
                e.getStatus()
        );
        metricsService.incrementReconciliation(
                e.getChannel(),
                result.name()
        );
        fallbackRouter.attemptFallback(e);
    }
}


