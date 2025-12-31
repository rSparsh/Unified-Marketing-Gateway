package com.project.unifiedMarketingGateway.processor;

import com.project.unifiedMarketingGateway.kafka.model.MarketingCampaignRequest;
import com.project.unifiedMarketingGateway.models.SendNotificationRequest;
import com.project.unifiedMarketingGateway.processor.sms.SmsRequestProcessor;
import com.project.unifiedMarketingGateway.processor.telegram.TelegramRequestProcessor;
import com.project.unifiedMarketingGateway.processor.whatsapp.WhatsappRequestProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationProcessor {

    @Autowired
    TelegramRequestProcessor telegramRequestProcessor;
    @Autowired
    WhatsappRequestProcessor whatsappRequestProcessor;
    @Autowired
    SmsRequestProcessor smsRequestProcessor;

    public void processKafkaEvent(MarketingCampaignRequest event) {
        SendNotificationRequest request = SendNotificationRequest.builder()
                .requestId(event.getRequestId())
                .textMessage(event.getTextMessage())
                .imageUrl(event.getImageUrl())
                .imageCaption(event.getImageCaption())
                .videoUrl(event.getVideoUrl())
                .videoCaption(event.getVideoCaption())
                .recipientList(event.getRecipientList())
                .mediaTypeList(event.getMediaTypeList())
                .build();

        switch(event.getChannel()){
            case TELEGRAM: telegramRequestProcessor.processNotificationRequest(request);
                break;
            case WHATSAPP: whatsappRequestProcessor.processNotificationRequest(request);
                break;
            case SMS: smsRequestProcessor.processNotificationRequest(request);
                break;
        }
    }
}
