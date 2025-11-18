package com.project.unifiedMarketingGateway.controllers;

import com.project.unifiedMarketingGateway.models.SendNotificationRequest;
import com.project.unifiedMarketingGateway.models.SendNotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class SendNotificationController {

    @PostMapping("/sendNotification")
    public SendNotificationResponse sendNotification(@RequestBody SendNotificationRequest request){
        log.info("SendNotificationController::sendNotification request: {}", request);

        SendNotificationResponse sendNotificationResponse = SendNotificationResponse.builder()
                .responseStatus("200")
                .customMessage("Notification sent")
                .build();
        return sendNotificationResponse;
    }

}
