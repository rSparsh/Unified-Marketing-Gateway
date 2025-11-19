package com.project.unifiedMarketingGateway.controllers;

import com.project.unifiedMarketingGateway.enums.ClientType;
import com.project.unifiedMarketingGateway.models.SendNotificationRequest;
import com.project.unifiedMarketingGateway.models.SendNotificationResponse;
import com.project.unifiedMarketingGateway.processor.TelegramRequestProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
public class SendNotificationController {

    @Autowired
    TelegramRequestProcessor telegramRequestProcessor;

    @PostMapping("/sendNotification")
    public SendNotificationResponse sendNotification(@RequestHeader ClientType clientType,
                                                     @RequestBody SendNotificationRequest request){
        log.info("SendNotificationController::sendNotification request: {}", request);

        SendNotificationResponse response = SendNotificationResponse.builder()
                .responseStatus("400")
                .customMessage("Notification not sent")
                .build();

        switch(clientType){
            case TELEGRAM: response = telegramRequestProcessor.processNotificationRequest(request);
            break;
        }

        return response;
    }
}
