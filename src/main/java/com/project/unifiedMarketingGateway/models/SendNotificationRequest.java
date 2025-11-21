package com.project.unifiedMarketingGateway.models;

import com.project.unifiedMarketingGateway.enums.MediaType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SendNotificationRequest {
    String textMessage;
    String imageUrl;
    String imageCaption;
    String videoUrl;
    String videoCaption;
    List<String> recipientList;
    List<MediaType> mediaTypeList;
}
