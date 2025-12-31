package com.project.unifiedMarketingGateway.kafka.model;

import com.project.unifiedMarketingGateway.enums.ClientType;
import com.project.unifiedMarketingGateway.enums.MediaType;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketingCampaignRequest {

    String requestId;
    @NonNull ClientType channel;
    String textMessage;
    String imageUrl;
    String imageCaption;
    String videoUrl;
    String videoCaption;
    @NotEmpty List<String> recipientList;
    @NotEmpty List<MediaType> mediaTypeList;

    long createdAtEpochMillis;
}
