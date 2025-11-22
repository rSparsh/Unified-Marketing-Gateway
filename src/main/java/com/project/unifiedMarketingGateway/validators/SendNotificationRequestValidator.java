package com.project.unifiedMarketingGateway.validators;

import com.project.unifiedMarketingGateway.models.SendNotificationRequest;

import java.util.List;

public interface SendNotificationRequestValidator {

    public List<String> validateSendNotificationRequest(SendNotificationRequest sendNotificationRequest);
}
