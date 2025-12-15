package com.project.unifiedMarketingGateway.processor;

import com.project.unifiedMarketingGateway.contexts.SendContext;
import com.project.unifiedMarketingGateway.entity.DeliveryStateEntity;
import com.project.unifiedMarketingGateway.enums.DeliveryStatus;
import com.project.unifiedMarketingGateway.repository.DeliveryStateRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeliveryStateService {

    @Autowired
    DeliveryStateRepository repository;

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
    public void markDeliveredByProviderMessageId(String providerMessageId) {
        repository.findByProviderMessageId(providerMessageId)
                .ifPresent(entity -> {
                    entity.setStatus(DeliveryStatus.DELIVERED);
                    entity.setUpdatedAtEpochMillis(System.currentTimeMillis());
                });
    }

    @Transactional
    public void markReadByProviderMessageId(String providerMessageId) {
        repository.findByProviderMessageId(providerMessageId)
                .ifPresent(entity -> {
                    entity.setStatus(DeliveryStatus.READ);
                    entity.setUpdatedAtEpochMillis(System.currentTimeMillis());
                });
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
}

