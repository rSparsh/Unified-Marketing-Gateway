package com.project.unifiedMarketingGateway.models;

import com.project.unifiedMarketingGateway.enums.WhatsappMessageStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WhatsappMessage {
     String waMessageId;          // wamid.* from WA response
     String recipient;           // phone number in international format
     String mediaType;           // "TEXT", "IMAGE", "VIDEO"
     long createdAtEpochMillis;
     Long lastUpdatedEpochMillis;
     WhatsappMessageStatus status;
     String errorCode;
     String errorDetails;
}
