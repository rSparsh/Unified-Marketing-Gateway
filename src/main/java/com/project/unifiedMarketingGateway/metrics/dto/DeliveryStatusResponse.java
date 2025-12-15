package com.project.unifiedMarketingGateway.metrics.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DeliveryStatusResponse {

    private String requestId;
    private List<ChannelDeliveryStatus> deliveries;
}

