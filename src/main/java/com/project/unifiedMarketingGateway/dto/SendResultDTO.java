package com.project.unifiedMarketingGateway.dto;

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
