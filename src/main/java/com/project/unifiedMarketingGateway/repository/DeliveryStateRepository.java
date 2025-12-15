package com.project.unifiedMarketingGateway.repository;

import com.project.unifiedMarketingGateway.entity.DeliveryStateEntity;
import com.project.unifiedMarketingGateway.enums.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryStateRepository
        extends JpaRepository<DeliveryStateEntity, Long> {

    Optional<DeliveryStateEntity> findByRequestIdAndChannelAndRecipientAndMediaType(
            String requestId,
            String channel,
            String recipient,
            String mediaType
    );

    List<DeliveryStateEntity> findByStatus(DeliveryStatus status);

    Optional<DeliveryStateEntity>
    findByProviderMessageId(String providerMessageId);

    List<DeliveryStateEntity> findByRequestId(String requestId);
}

