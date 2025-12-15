package com.project.unifiedMarketingGateway.builders;

import com.project.unifiedMarketingGateway.models.SendNotificationResponse;
import org.springframework.stereotype.Service;

import static com.project.unifiedMarketingGateway.constants.Constants.STATUS_CODE_200;
import static com.project.unifiedMarketingGateway.constants.Constants.STATUS_CODE_400;

@Service
public class SendNotificationResponseBuilder {

    public SendNotificationResponse buildSuccessResponse(String responseBody, String requestId)
    {
        return buildResponse(STATUS_CODE_200, responseBody, requestId);
    }

    public SendNotificationResponse buildFailureResponse(String responseBody, String requestId)
    {
        return buildResponse(STATUS_CODE_400, responseBody, requestId);
    }

    private SendNotificationResponse buildResponse(String status, String responseBody, String requestId)
    {
        return SendNotificationResponse.builder()
                .responseStatus(status)
                .customMessage(responseBody)
                .requestId(requestId)
                .build();
    }
}
