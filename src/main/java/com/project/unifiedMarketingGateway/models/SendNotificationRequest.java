package com.project.unifiedMarketingGateway.models;

import com.project.unifiedMarketingGateway.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {
    String textMessage;
    String imageUrl;
    String imageCaption;
    String videoUrl;
    String videoCaption;
    List<String> recipientList;
    List<MediaType> mediaTypeList;
}
