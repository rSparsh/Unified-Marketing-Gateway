package com.project.unifiedMarketingGateway.repository;

import com.project.unifiedMarketingGateway.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyRepository
        extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByRequestIdAndChannelAndRecipientAndMediaType(
            String requestId,
            String channel,
            String recipient,
            String mediaType
    );
}

