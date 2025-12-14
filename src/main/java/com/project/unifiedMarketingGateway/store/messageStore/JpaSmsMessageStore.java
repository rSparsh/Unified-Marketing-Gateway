package com.project.unifiedMarketingGateway.store.messageStore;

import com.project.unifiedMarketingGateway.entity.SmsMessageEntity;
import com.project.unifiedMarketingGateway.enums.SmsMessageStatus;
import com.project.unifiedMarketingGateway.repository.SmsMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JpaSmsMessageStore implements SmsMessageStore {

    private final SmsMessageRepository repository;

    @Override
    public void storeQueued(String recipient) {
        SmsMessageEntity entity = SmsMessageEntity.builder()
                .recipient(recipient)
                .status(SmsMessageStatus.QUEUED)
                .createdAtEpochMillis(System.currentTimeMillis())
                .build();
        repository.save(entity);
    }

    @Override
    public void storeSent(String sid, String recipient) {
        SmsMessageEntity entity = SmsMessageEntity.builder()
                .sid(sid)
                .recipient(recipient)
                .status(SmsMessageStatus.SENT)
                .createdAtEpochMillis(System.currentTimeMillis())
                .build();
        repository.save(entity);
    }

    @Override
    public void storeFailed(String recipient, String errorMessage) {
        SmsMessageEntity entity = SmsMessageEntity.builder()
                .recipient(recipient)
                .status(SmsMessageStatus.FAILED)
                .errorMessage(errorMessage)
                .createdAtEpochMillis(System.currentTimeMillis())
                .build();
        repository.save(entity);
    }
}
