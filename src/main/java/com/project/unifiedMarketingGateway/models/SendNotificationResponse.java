package com.project.unifiedMarketingGateway.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SendNotificationResponse {
    String responseStatus;
    String customMessage;
}
