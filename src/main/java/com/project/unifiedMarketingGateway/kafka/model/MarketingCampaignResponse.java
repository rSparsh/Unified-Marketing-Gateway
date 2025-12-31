package com.project.unifiedMarketingGateway.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketingCampaignResponse {
    boolean isQueued;
    String requestId;
}
