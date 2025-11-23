package com.project.unifiedMarketingGateway.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.unifiedMarketingGateway.enums.ClientType;
import com.project.unifiedMarketingGateway.enums.MediaType;
import com.project.unifiedMarketingGateway.models.SendNotificationRequest;
import com.project.unifiedMarketingGateway.models.SendNotificationResponse;
import com.project.unifiedMarketingGateway.processor.telegram.TelegramRequestProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@ExtendWith(MockitoExtension.class)
public class SendNotificationControllerTest {

    @Mock
    private TelegramRequestProcessor telegramRequestProcessor;

    @InjectMocks
    private SendNotificationController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private SendNotificationRequest request;
    private SendNotificationResponse mockTelegramResponse;

    @BeforeEach
    void setUp() {
        // Build standalone MockMvc for the controller under test
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();

        request = SendNotificationRequest.builder()
                .mediaTypeList(List.of(MediaType.TEXT))
                .textMessage("hello")
                .build();

        mockTelegramResponse = SendNotificationResponse.builder()
                .responseStatus("200")
                .customMessage("Notification sent to Telegram")
                .build();
    }

    @Test
    void testSendNotification_Telegram() throws Exception {
        // Arrange
        when(telegramRequestProcessor.processNotificationRequest(any(SendNotificationRequest.class)))
                .thenReturn(mockTelegramResponse);

        // Act
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/sendNotification")
                        .header("clientType", "TELEGRAM")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        // Assert: check response body (use objectMapper to avoid brittle string matching)
        String responseContent = result.getResponse().getContentAsString();
        String expectedJson = objectMapper.writeValueAsString(mockTelegramResponse);
        assertEquals(expectedJson, responseContent);

        // Verify interaction: ensure the processor was called (use any() to match the deserialized object)
        verify(telegramRequestProcessor, times(1)).processNotificationRequest(any(SendNotificationRequest.class));
    }


    @Test
    void testSendNotification_UnsupportedClientType() throws Exception {
        // Act
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/sendNotification")
                        .header("clientType", "EMAIL")
                        .contentType("application/json")
                        .content(asJsonString(request)))
                .andReturn();

        // Verify no interaction
        verify(telegramRequestProcessor, never()).processNotificationRequest(any());
    }

    // Helper method to convert object to JSON string
    private String asJsonString(Object obj) throws Exception {
        return new ObjectMapper().writeValueAsString(obj);
    }
}