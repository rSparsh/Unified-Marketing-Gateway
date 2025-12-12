package com.project.unifiedMarketingGateway.store.messageStore;

import com.project.unifiedMarketingGateway.entity.WhatsappMessageEntity;
import com.project.unifiedMarketingGateway.enums.WhatsappMessageStatus;
import com.project.unifiedMarketingGateway.models.WhatsappMessage;
import com.project.unifiedMarketingGateway.repository.WhatsappMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JpaWhatsappMessageStore implements WhatsappMessageStore {

    private final WhatsappMessageRepository repository;

    @Override
    @Transactional
    public void saveOutbound(WhatsappMessage message) {
        if (message == null || message.getWaMessageId() == null) {
            log.warn("Attempt to save outbound with null waMessageId - ignoring");
            return;
        }

        WhatsappMessageEntity entity = WhatsappMessageEntity.builder()
                .waMessageId(message.getWaMessageId())
                .recipient(message.getRecipient())
                .mediaType(message.getMediaType())
                .requestId(message.getRequestId())
                .createdAtEpochMillis(message.getCreatedAtEpochMillis())
                .lastUpdatedEpochMillis(message.getLastUpdatedEpochMillis())
                .status(message.getStatus())
                .errorCode(message.getErrorCode())
                .errorDetails(message.getErrorDetails())
                .build();

        repository.save(entity);
    }

    @Override
    @Transactional
    public void updateStatus(String waMessageId,
            String recipient,
            WhatsappMessageStatus status,
            String errorCode,
            String errorDetails) {
        if (waMessageId == null) return;

        repository.findByWaMessageId(waMessageId).ifPresentOrElse(entity -> {
            entity.setStatus(status);
            entity.setLastUpdatedEpochMillis(System.currentTimeMillis());
            entity.setErrorCode(errorCode);
            entity.setErrorDetails(errorDetails);
            repository.save(entity);
        }, () -> {
            // If the send path didn't create a record (possible), create a minimal one.
            WhatsappMessageEntity entity = WhatsappMessageEntity.builder()
                    .waMessageId(waMessageId)
                    .recipient(recipient)
                    .createdAtEpochMillis(System.currentTimeMillis())
                    .lastUpdatedEpochMillis(System.currentTimeMillis())
                    .status(status)
                    .errorCode(errorCode)
                    .errorDetails(errorDetails)
                    .build();
            repository.save(entity);
            log.info("Created minimal record for waMessageId={} from webhook", waMessageId);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WhatsappMessage> findByWaMessageId(String waMessageId) {
        return repository.findByWaMessageId(waMessageId).map(this::toDomain);
    }

    private WhatsappMessage toDomain(WhatsappMessageEntity e) {
        return WhatsappMessage.builder()
                .waMessageId(e.getWaMessageId())
                .recipient(e.getRecipient())
                .mediaType(e.getMediaType())
                .requestId(e.getRequestId())
                .createdAtEpochMillis(e.getCreatedAtEpochMillis() == null ? 0L : e.getCreatedAtEpochMillis())
                .lastUpdatedEpochMillis(e.getLastUpdatedEpochMillis())
                .status(e.getStatus())
                .errorCode(e.getErrorCode())
                .errorDetails(e.getErrorDetails())
                .build();
    }
}