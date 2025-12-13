package com.project.unifiedMarketingGateway.store.messageStore;

public interface SmsMessageStore {

    void storeQueued(String recipient);

    void storeSent(String sid, String recipient);

    void storeFailed(String recipient, String errorMessage);
}
