package com.project.unifiedMarketingGateway.responseBuilders;

import com.project.unifiedMarketingGateway.models.SendNotificationResponse;
import org.springframework.stereotype.Service;

import static com.project.unifiedMarketingGateway.constants.Constants.STATUS_CODE_200;
import static com.project.unifiedMarketingGateway.constants.Constants.STATUS_CODE_400;

@Service
public class SendNotificationResponseBuilder {

    public SendNotificationResponse buildSuccessResponse(String responseBody)
    {
        return buildResponse(STATUS_CODE_200, responseBody);
    }

    public SendNotificationResponse buildFailureResponse(String responseBody)
    {
        return buildResponse(STATUS_CODE_400, responseBody);
    }

    private SendNotificationResponse buildResponse(String status, String responseBody)
    {
        return SendNotificationResponse.builder()
                .responseStatus(status)
                .customMessage(responseBody)
                .build();
    }
}
