package com.project.unifiedMarketingGateway.metrics.dto;

import com.project.unifiedMarketingGateway.enums.DeliveryStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChannelDeliveryStatus {

    private String channel;
    private String recipient;
    private String mediaType;
    private DeliveryStatus status;
    private String providerMessageId;
    private String failureReason;
    private Long updatedAtEpochMillis;
}