package com.project.unifiedMarketingGateway.processor;

import com.project.unifiedMarketingGateway.contexts.SendContext;
import com.project.unifiedMarketingGateway.entity.DeliveryStateEntity;
import com.project.unifiedMarketingGateway.enums.DeliveryStatus;
import com.project.unifiedMarketingGateway.repository.DeliveryStateRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DeliveryStateService {

    @Autowired
    DeliveryStateRepository repository;

    private static final Map<DeliveryStatus, Integer> PRECEDENCE = Map.of(
            DeliveryStatus.CREATED, 0,
            DeliveryStatus.QUEUED, 1,
            DeliveryStatus.SENT, 2,
            DeliveryStatus.DELIVERED, 3,
            DeliveryStatus.READ, 4,
            DeliveryStatus.FAILED, -1 // terminal but special
    );

    @Transactional
    public void markQueued(SendContext ctx) {
        upsert(ctx, DeliveryStatus.QUEUED, null, null);
    }

    @Transactional
    public void markSent(SendContext ctx, String providerMessageId) {
        upsert(ctx, DeliveryStatus.SENT, providerMessageId, null);
    }

    @Transactional
    public void markFailed(SendContext ctx, String reason) {
        upsert(ctx, DeliveryStatus.FAILED, null, reason);
    }

    private void upsert(
            SendContext ctx,
            DeliveryStatus status,
            String providerMessageId,
            String failureReason
    ) {
        DeliveryStateEntity entity = repository
                .findByRequestIdAndChannelAndRecipientAndMediaType(
                        ctx.getRequestId(),
                        ctx.getChannel(),
                        ctx.getRecipient(),
                        ctx.getMethod()
                )
                .orElseGet(() ->
                        DeliveryStateEntity.builder()
                                .requestId(ctx.getRequestId())
                                .channel(ctx.getChannel())
                                .recipient(ctx.getRecipient())
                                .mediaType(ctx.getMethod())
                                .createdAtEpochMillis(System.currentTimeMillis())
                                .build()
                );

        entity.setStatus(status);
        entity.setProviderMessageId(providerMessageId);
        entity.setFailureReason(failureReason);
        entity.setUpdatedAtEpochMillis(System.currentTimeMillis());

        repository.save(entity);
    }

    @Transactional
    public void markFailedByProviderMessageId(
            String providerMessageId,
            String reason
    ) {
        repository.findByProviderMessageId(providerMessageId)
                .ifPresent(entity -> {
                    entity.setStatus(DeliveryStatus.FAILED);
                    entity.setFailureReason(reason);
                    entity.setUpdatedAtEpochMillis(System.currentTimeMillis());
                });
    }

    @Transactional
    public void updateFromWebhook(
            String providerMessageId,
            DeliveryStatus incomingStatus
    ) {
        repository.findByProviderMessageId(providerMessageId)
                .ifPresent(entity -> {

                    DeliveryStatus current = entity.getStatus();

                    // Ignore duplicates or backward transitions
                    if (PRECEDENCE.get(incomingStatus) <= PRECEDENCE.get(current)) {
                        return;
                    }

                    entity.setStatus(incomingStatus);
                    entity.setUpdatedAtEpochMillis(System.currentTimeMillis());
                });
    }
}

