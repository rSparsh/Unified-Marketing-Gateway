package com.project.unifiedMarketingGateway.processor;

import com.project.unifiedMarketingGateway.enums.MediaType;
import com.project.unifiedMarketingGateway.models.SendNotificationRequest;
import com.project.unifiedMarketingGateway.models.SendNotificationResponse;

import java.util.List;

public interface RequestProcessorInterface {

    public SendNotificationResponse processNotificationRequest(SendNotificationRequest sendNotificationRequest);
}
