package com.project.unifiedMarketingGateway.processor;

import com.project.unifiedMarketingGateway.models.SendNotificationRequest;
import com.project.unifiedMarketingGateway.models.SendNotificationResponse;
import org.springframework.stereotype.Service;

@Service
public class TelegramRequestProcessor implements RequestProcessorInterface{

    @Override
    public SendNotificationResponse processNotificationRequest(SendNotificationRequest sendNotificationRequest) {
        SendNotificationResponse response = SendNotificationResponse.builder()
                .responseStatus("200")
                .customMessage("Notification sent")
                .build();

        return response;
    }
}
