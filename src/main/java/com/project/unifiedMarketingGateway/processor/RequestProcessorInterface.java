package com.project.unifiedMarketingGateway.processor;

import com.project.unifiedMarketingGateway.models.SendNotificationRequest;
import com.project.unifiedMarketingGateway.models.SendNotificationResponse;

public interface RequestProcessorInterface {

    public SendNotificationResponse processNotificationRequest(SendNotificationRequest sendNotificationRequest);
}
