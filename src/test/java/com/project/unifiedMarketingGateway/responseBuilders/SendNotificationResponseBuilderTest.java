package com.project.unifiedMarketingGateway.responseBuilders;

import com.project.unifiedMarketingGateway.builders.SendNotificationResponseBuilder;
import com.project.unifiedMarketingGateway.models.SendNotificationResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SendNotificationResponseBuilderTest {

    private final SendNotificationResponseBuilder responseBuilder = new SendNotificationResponseBuilder();

    @Test
    public void testBuildSuccessResponse() {
        String expectedMessage = "Test success message";
        SendNotificationResponse response = responseBuilder.buildSuccessResponse(expectedMessage, "12345");

        assertEquals("200", response.getResponseStatus());
        assertEquals(expectedMessage, response.getCustomMessage());
        assertEquals("12345", response.getRequestId());
    }

    @Test
    public void testBuildFailureResponse() {
        String expectedMessage = "Test failure message";
        SendNotificationResponse response = responseBuilder.buildFailureResponse(expectedMessage, "12345");

        assertEquals("400", response.getResponseStatus());
        assertEquals(expectedMessage, response.getCustomMessage());
        assertEquals("12345", response.getRequestId());
    }
}
