package com.project.unifiedMarketingGateway.processor;

import com.project.unifiedMarketingGateway.entity.IdempotencyRecord;
import com.project.unifiedMarketingGateway.enums.IdempotencyStatus;
import com.project.unifiedMarketingGateway.repository.IdempotencyRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class IdempotencyService {

    @Autowired IdempotencyRepository repository;

    @Transactional
    public boolean tryStart(
            String requestId,
            String channel,
            String recipient,
            String mediaType
    ) {
        Optional<IdempotencyRecord> existing =
                repository.findByRequestIdAndChannelAndRecipientAndMediaType(
                        requestId, channel, recipient, mediaType
                );

        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();

            if (record.getStatus() == IdempotencyStatus.COMPLETED) {
                return false;
            }

            if (record.getStatus() == IdempotencyStatus.IN_PROGRESS) {
                return false;
            }

            // FAILED → reuse the same row
            record.setStatus(IdempotencyStatus.IN_PROGRESS);
            record.setCreatedAtEpochMillis(System.currentTimeMillis());
            record.setCompletedAtEpochMillis(null);
            repository.save(record);

            return true;
        }

        // First-ever attempt → insert
        IdempotencyRecord record = IdempotencyRecord.builder()
                .requestId(requestId)
                .channel(channel)
                .recipient(recipient)
                .mediaType(mediaType)
                .status(IdempotencyStatus.IN_PROGRESS)
                .createdAtEpochMillis(System.currentTimeMillis())
                .build();

        repository.save(record);
        return true;
    }

    @Transactional
    public void markCompleted(
            String requestId,
            String channel,
            String recipient,
            String mediaType
    ) {
        repository.findByRequestIdAndChannelAndRecipientAndMediaType(
                requestId, channel, recipient, mediaType
        ).ifPresent(record -> {
            record.setStatus(IdempotencyStatus.COMPLETED);
            record.setCompletedAtEpochMillis(System.currentTimeMillis());
        });
    }

    @Transactional
    public void markFailed(
            String requestId,
            String channel,
            String recipient,
            String mediaType
    ) {
        repository.findByRequestIdAndChannelAndRecipientAndMediaType(
                requestId, channel, recipient, mediaType
        ).ifPresent(record -> record.setStatus(IdempotencyStatus.FAILED));
    }
}

