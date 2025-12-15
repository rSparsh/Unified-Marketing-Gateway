package com.project.unifiedMarketingGateway.metrics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SendResultDTO {
    String chatId;
    boolean success;
    String responseBody;
    String errorMessage;
}
